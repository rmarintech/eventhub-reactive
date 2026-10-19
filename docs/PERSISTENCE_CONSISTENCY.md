# Persistence and Consistency — EventHub

R2DBC, JPA, optimistic locking, local transactions, duplicate protection and synchronous compensation. For full HTTP failure scenarios see [MICROSERVICES_COMMUNICATION.md](MICROSERVICES_COMMUNICATION.md); for resilience decorators see [RESILIENCE.md](RESILIENCE.md).

---

## Reactive PostgreSQL Persistence with R2DBC

The Event module now has real reactive persistence backed by PostgreSQL
17 and Spring Data R2DBC. PostgreSQL runs locally in Docker Compose and
the application connects through an R2DBC URL. The schema is initialized
from `src/main/resources/schema.sql`.

The persistence boundary is now:

``` text
Event                                  DOMAIN
  ↓
EventRepository                        OUTPUT PORT
  ↓
R2dbcEventRepositoryAdapter            OUTPUT ADAPTER
  ↓
SpringDataEventRepository
  ↓
ReactiveCrudRepository<EventEntity, UUID>
  ↓
R2DBC PostgreSQL driver
  ↓
PostgreSQL
```

The domain remains persistence-independent. `EventEntity` is an
infrastructure persistence model annotated with `@Table("events")`;
domain Value Objects are flattened into relational columns such as
total/available capacity and amount/currency.

The adapter performs explicit mapping in both directions:

``` text
Event → EventEntity → PostgreSQL
PostgreSQL → EventEntity → Event.rehydrate(...) → Event
```

`Event.rehydrate(...)` reconstructs an existing Aggregate while
preserving its persisted ID, status and available capacity. It is
distinct from `Event.create(...)`, which represents creation of a new
Aggregate.

---

## Create vs Update Persistence Intent

Because `Event.create()` generates its UUID before persistence, a
non-null `@Id` alone cannot tell Spring Data whether the row is new.
This was observed in practice: Spring Data treated a new entity as an
update, the HTTP request returned successfully, but no row was inserted.

`EventEntity` therefore implements `Persistable<UUID>` and exposes
transient persistence metadata through `isNew`. The flag is not stored
in PostgreSQL.

The application-owned repository port now makes persistence intent
explicit:

``` text
create(Event) → new Event → isNew = true  → INSERT
update(Event) → existing Event → isNew = false → UPDATE
```

This keeps the persistence concern out of the domain.
`CreateEventService` calls `create()`, while publication and capacity
reservation call `update()`.

The in-memory adapter implements the same port, although both operations
use `Map.put(...)` internally because a `HashMap` does not need to
distinguish SQL INSERT from UPDATE.

The implementation was validated with the full green test suite and
manually against PostgreSQL: creating Events produced persisted `DRAFT`
rows and publishing an Event updated the same row to `PUBLISHED`.

Reactive transactions, database migrations and PostgreSQL Testcontainers
have not yet been implemented.

---
## Booking PostgreSQL Persistence

The Booking module now also persists Bookings reactively in PostgreSQL
through its own persistence adapter.

The already studied cross-module boundary is preserved:

``` text
CreateBookingService
    ↓
ReserveEventPlacesUseCase
    ↓
Event module updates Event capacity
    ↓
BookingRepository
    ↓
Booking persistence adapter
    ↓
PostgreSQL
```

The Booking application layer still does not access `EventRepository`
directly. Event owns its persistence, and Booking coordinates with Event
through the Event application input port.

The complete flow was manually verified: creating a Booking for 3 places
persisted the Booking and changed the corresponding Event availability
from `20` to `17`.

At this point these are two successfully composed reactive persistence
operations. Atomicity across both database writes has not yet been
implemented or studied; reactive transactions remain a later step.

---
## Concurrent Booking and Optimistic Locking

A concurrency problem was reproduced around Event capacity. Two booking
operations can read the same persisted Event state before either update
has completed.

``` text
Event availability = 3

Booking A reads available = 3
Booking B reads available = 3

Booking A reserves 2
Booking B reserves 2
```

Each Aggregate instance can satisfy its domain invariant because each
was created from a snapshot that still contained 3 available places.
Without concurrency control, both operations could therefore proceed
from stale state and cause a lost update / business overselling problem.

The distinction studied is:

``` text
Domain invariant
    → protects one Aggregate instance

Concurrency control
    → protects persisted state when multiple operations race
```

The Event persistence model now uses optimistic locking with a version
value. The Event Aggregate preserves that version when existing state is
rehydrated, and the R2DBC persistence entity maps it using Spring Data's
`@Version`.

``` text
both operations read Event version N
        ↓
first UPDATE succeeds
        ↓
database version becomes N + 1
        ↓
second UPDATE still expects version N
        ↓
optimistic locking failure
```

This prevents a stale Event snapshot from silently overwriting the
update that won the race.

A focused concurrent-booking test reproduces the race and verifies the
optimistic-locking behaviour. The full test suite is green.

The operation that loses the optimistic-locking race is now handled by
the application. A persistence-specific optimistic-locking failure is
translated at the infrastructure boundary into
`ConcurrentUpdateException`, an application-level exception. This keeps
Spring Data details out of the application layer.

---
## Recovering the Booking That Loses the Race

An optimistic-locking conflict means that the Event state used by the
losing operation is stale. It does not necessarily mean that the Event
is already full.

The recovery policy studied and implemented is:

``` text
reserve using current Event snapshot
        ↓
concurrent update detected
        ↓
ConcurrentUpdateException
        ↓
re-read Event from persistence
        ↓
re-evaluate Event.reservePlaces(...)
        ↓
retry the update once
```

Re-reading is important because the domain rule must be evaluated
against the latest persisted capacity.

For example:

``` text
available = 5
Booking A reserves 1
Booking B wants 2 and loses the version race
        ↓
re-read → available = 4
        ↓
Booking B can still reserve 2
```

But:

``` text
available = 3
Booking A reserves 2
Booking B wants 2 and loses the version race
        ↓
re-read → available = 1
        ↓
Event.reservePlaces(2) rejects the reservation
```

The retry is deliberately bounded to one additional attempt rather than
being an unlimited retry loop.

The concurrency integration test now verifies the resulting business
state for two concurrent Bookings of 2 places against an Event with
capacity 3. After the race settles, exactly one Booking is persisted and
the Event has 1 available place.

To verify persisted Bookings by Event, the Booking application now
exposes a query use case backed by
`BookingRepository.findByEventId(...)`. The R2DBC adapter delegates that
query to a Spring Data derived query and maps the resulting persistence
entities back to domain `Booking` objects.

The test composes the concurrent attempt with the later verification
using Reactor. `then(...)` waits for the first Publisher to terminate
successfully while discarding its emitted value, then subscribes to the
Publisher that reads the final persisted state. `Flux.count()` converts
the Booking query into a `Mono<Long>` so the final Booking count can be
asserted together with the persisted Event.

Reactive transaction/atomicity across the Event update and Booking
insert has still not been implemented or studied.

------------------------------------------------------------------------

This is the current stopping point of the DDD and architecture
documentation.

---

## Transaction boundary for Booking creation

Booking creation coordinates two persisted changes: reserving capacity
on the Event Aggregate and creating the Booking Aggregate. These writes
form one application-level business operation.

``` text
reserve Event capacity
        ↓
persist Event
        ↓
persist Booking
        ↓
all succeed → COMMIT
any fails   → ROLLBACK
```

A test forces Booking persistence to fail after the Event update and
verifies that the Event capacity returns to its previous persisted
value. This prevents a partial state where capacity has been consumed
without a corresponding Booking.

Optimistic locking and transaction atomicity solve different problems:
optimistic locking detects stale concurrent writes, while the
transaction provides all-or-nothing persistence across Event and
Booking. Both mechanisms are therefore retained.

---
## JPA/PostgreSQL persistence and Flyway

The independent Booking service persists to PostgreSQL 17 through Spring Data JPA / Hibernate. `BookingJpaEntity` stores UUID fields (`booking_id`, `event_id`, `user_id`) and a string `status`; `BookingJpaEntity.fromDomain()` and `toDomain()` convert between persistence and the immutable domain. `Booking.rehydrate(...)` restores a booking with its existing ID and state; domain creation still starts in `PENDING`.

`SpringDataBookingRepository extends JpaRepository<BookingJpaEntity, UUID>`. The `JpaBookingRepositoryAdapter` implements `BookingRepository`, including the Spring Data derived query `existsByEventIdAndUserIdAndStatusNot(..., CANCELLED)`. No JPA annotations leak into domain or application.

Flyway migration `V1__create_bookings_table.sql` creates the database schema. `spring.jpa.hibernate.ddl-auto=none` prevents Hibernate from owning schema creation. PostgreSQL is also available locally with Docker Compose, but the integration tests use a separate Testcontainers PostgreSQL 17 database.

Verified tests cover round-trip entity/domain mapping, insert and lookup, active lookup, exclusion of cancelled bookings, and updating an existing booking from `PENDING` to `CONFIRMED`. Integration tests used `EntityManager.flush()` followed by `clear()` so reads hit the database instead of only the persistence context. `@Transactional` test methods roll back automatically for repository-level tests; `flush()` is **not** `commit()`.

---

## Transaction boundaries: decorator pattern

To maintain an application layer with **no Spring dependency**, transaction configuration lives in an infrastructure decorator, rather than annotating `BookingService`:

```text
Spring-managed BookingUseCase proxy
    → TransactionalBookingUseCase (@Transactional)
    → BookingService (pure Java)
    → BookingRepository port
    → JpaBookingRepositoryAdapter
    → PostgreSQL
```

`BookingConfiguration` constructs `new BookingService(repository)` and returns `new TransactionalBookingUseCase(service)` as a single Spring-managed `BookingUseCase` bean. Spring AOP intercepts external calls to that bean and manages transaction boundaries. Writes (`createBooking`, `confirmBooking`, `cancelBooking`) use `@Transactional`; reads (`getBooking`) use `@Transactional(readOnly = true)`.

This is the **Decorator** pattern: the object exposes the same `BookingUseCase` interface, delegates business logic, and adds cross-cutting transactional behavior. A manually instantiated decorator outside Spring would not by itself activate the Spring proxy.

### Rollback proof with Testcontainers

A dedicated integration test, `TransactionalBookingRollbackIT`, registers a test-specific `@Primary` `BookingUseCase` bean that wraps a failing delegate in the same decorator. The delegate calls `repository.save(new Booking(...))` and then throws `IllegalStateException`. The test deliberately **has no `@Transactional` annotation**; after asserting the exception, it queries the repository and expects no active booking.

The positive run with the decorator annotation returned green (rollback). As a negative control, temporarily removing `@Transactional` from the decorator left the booking committed by the repository's own transaction; `assertFalse(exists)` failed with `Expected: false, Actual: true`. Restoring the annotation made the test green again. This shows the test detects the transaction boundary rather than only observing Spring Test's end-of-test cleanup.

`TransactionalBookingUseCaseIT` separately verifies that a successful `createBooking` can be read afterward without a test-wide transaction. Persistence on its own does **not** prove that the decorator opened the transaction, because Spring Data repository writes can be transactional independently; the negative-control rollback test provides stronger evidence.

### Concurrency principle (subsequently addressed)

A service-level transaction at the usual `READ_COMMITTED` isolation does **not** make `existsActiveBooking(eventId, userId)` followed by `save()` a database-level uniqueness guarantee. Two concurrent transactions can both observe no active booking and attempt to insert separate IDs. Flyway V2 now establishes the PostgreSQL partial unique index described below. A simple `UNIQUE(event_id, user_id)` would also block rebooking after cancellation.

---

## Active-booking uniqueness and persistence error translation

`V2__unique_active_booking.sql` adds:

```sql
CREATE UNIQUE INDEX uq_bookings_active_event_user
ON bookings (event_id, user_id)
WHERE status <> 'CANCELLED';
```

The database therefore rejects multiple active (`PENDING` / `CONFIRMED`) reservations for the same event/user while preserving the ability to book again after cancellation. Integration tests against PostgreSQL Testcontainers passed for `PENDING + PENDING` (rejected) and `CANCELLED + PENDING` (allowed). This constraint, not the application's pre-check or `@Transactional` alone, provides the concurrency-safe uniqueness guarantee.

`JpaBookingRepositoryAdapter.save()` uses Spring Data `saveAndFlush()` to trigger the database check while the adapter can handle the failure. `flush()` executes pending SQL but does **not** commit the surrounding transaction. The adapter catches Spring's `DataIntegrityViolationException`, searches its cause chain for Hibernate's `ConstraintViolationException`, and translates **only** constraint `uq_bookings_active_event_user` into the application exception `BookingAlreadyExistsException`. Unrelated integrity exceptions remain unchanged; a Mockito unit test verified this using a different constraint name. The inbound `GlobalExceptionHandler` already maps the application exception to HTTP `409 Conflict`.

The regular application-level `existsActiveBooking()` pre-check is retained for a clear business response in the common sequential case, but is **not** a race-condition guarantee.

---

## Concurrent-start and end-to-end tests

`ConcurrentBookingIT` uses a two-thread `ExecutorService`, a readiness `CountDownLatch(2)` and a start `CountDownLatch(1)` to release two independent booking requests for the same event/user. Without test-level `@Transactional`, each call runs through the Spring-managed transactional decorator. The test passed with exactly one successful booking and one `BookingAlreadyExistsException`, and an active booking present afterward. **Limitation:** synchronizing the start does not force both threads to observe `existsActiveBooking() == false`; a deterministic barrier at that read remains an optional stronger race test.

`TransactionalBookingUseCaseIT` (without test-level `@Transactional`) verifies committed creation and the valid transitions `PENDING → CONFIRMED`, `PENDING → CANCELLED` and `PENDING → CONFIRMED → CANCELLED` against PostgreSQL. It checks the persisted state and that the original immutable domain instances retain their prior statuses.

`BookingHttpIT` uses `@SpringBootTest`, `@AutoConfigureMockMvc`, Testcontainers and the real application beans (no mocks). The end-to-end-in-process HTTP tests passed for initial `POST /api/bookings` → `201 Created` with `PENDING` persisted, and a repeated request → `409 Conflict` with `BOOKING_ALREADY_EXISTS`. The repeated request exercises the sequential duplicate path; the concurrent-start test exercises concurrent application calls rather than HTTP concurrency.

**Next architectural learning track:** communication between the reactive Event module/service and the imperative Booking service. Remote Event-capacity coordination, failure handling, Kafka, outbox and distributed consistency are not yet implemented.

---

## Synchronous Event capacity reservation and Booking compensation

## 59.1 Domain invariants and HTTP boundary

`Event` owns its `Capacity` value object. `Capacity.reserve(places)` returns a new immutable `Capacity` or raises the **domain** `InsufficientCapacityException` when insufficient places remain; `release(places)` uses `ExcessiveCapacityException` when the release exceeds occupied places. Domain code does not depend on HTTP. `EventExceptionHandler` maps both domain conflicts and `ConcurrentUpdateException` to HTTP `409 Conflict`. The WebFlux Event API exposes `POST /events/{id}/reserve/{places}/places` and `POST /events/{id}/release/{places}/places`. Reactive R2DBC persistence uses Event optimistic locking and one bounded retry for `ConcurrentUpdateException`. A WebTestClient test verified insufficient capacity returns 409 without changing available places.

## 59.2 Current Booking hexagonal composition

The independent, **imperative** Booking Service uses two distinct output ports: `EventAvailabilityPort` retains the previously implemented read-only GET availability/resilience path, while the **current create-booking flow** uses `EventCapacityPort` (`reservePlaces`, `releasePlaces`) implemented by `RestEventCapacityAdapter` with blocking `RestClient`. `BookingService` now checks for active duplicate bookings and saves them; it does **not** call `isAvailable()`.

```text
POST /api/bookings
  → BookingController
    → CompensatingBookingUseCase           [outer coordinator, no @Transactional]
      → EventCapacityPort.reservePlaces(eventId, 1)
        → RestEventCapacityAdapter → POST Event Service /events/{id}/reserve/1/places
      → TransactionalBookingUseCase       [separate Spring bean; @Transactional proxy]
        → BookingService → BookingRepository → JPA / PostgreSQL
      → on Booking creation/commit failure: EventCapacityPort.releasePlaces(eventId, 1)
```

The transaction decorator is a distinct Spring bean so that its `@Transactional` proxy commits (or rolls back) **before** the outer coordinator observes the outcome. A successful booking is `PENDING`. When reservation fails, the delegate is not invoked and no release is attempted. When the delegate throws after a successful reservation, the coordinator attempts a compensating release and propagates the original failure, preserving it if compensation also fails. This is a **minimal, synchronous Saga-like compensation**, not a distributed atomic transaction.

The REST adapter translates reserve 409 to `EventNotAvailableException`, 404 to `EventNotFoundException`, and HTTP 5xx/transport failures to `EventServiceUnavailableException`. During `releasePlaces`, HTTP 409 currently propagates as `HttpClientErrorException.Conflict`, rather than incorrectly labeling excess release as unavailable capacity. No automatic Retry decorator wraps these **mutating POST** calls; the existing `Bulkhead → Circuit Breaker → Retry → REST` chain remains applicable to **GET availability** only.

## 59.3 Verification and limitations

- Event WebTestClient insufficient-capacity test passed (409; available count unchanged).
- `RestEventCapacityAdapterTest`: eight tests passed covering reserve/release HTTP success, 404, 409 and 500, including the distinct release-409 behavior. These use `MockRestServiceServer`, not a live Event Service.
- `CompensatingBookingUseCaseTest`: three Mockito tests passed for success, compensation after delegate failure, and no compensation when initial reservation fails.
- Booking HTTP/JPA/concurrency tests were adapted from `EventAvailabilityPort` mocks to `EventCapacityPort`; the end-to-end two-service POST workflow has **not yet** been manually verified.

**Known limitations:** no shared transaction across the two PostgreSQL databases; no durable compensation retry, reservation ID, idempotency key, outbox or guaranteed resolution of ambiguous network timeouts. A failure or timeout during release can leave capacity inconsistent, and a timeout during reserve can leave an uncertain outcome. Cancellation currently does not trigger remote capacity release. These are deferred design tasks, not established production guarantees.


## Manual cross-service consistency evidence — 

With Event Service and independent Booking Service running against their actual persistence, a new `PENDING` Booking returned HTTP 201 and Event availability fell from 20 to 19. Repeating the same booking returned `409 BOOKING_ALREADY_EXISTS` and a later Event GET still reported 19 available places. This is **consistent with** the synchronous `CompensatingBookingUseCase` reserving and releasing on duplicate failure. The test did not independently capture the intermediate Event state or inject a controlled JPA failure after a confirmed remote reservation. These results **do not** establish distributed atomicity, durable compensation or idempotency. See [`MICROSERVICES_COMMUNICATION.md`](MICROSERVICES_COMMUNICATION.md).
