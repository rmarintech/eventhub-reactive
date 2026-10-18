# EventHub Reactive


**Current milestone — 18 October 2026:** Booking Service now implements **Transactional Outbox and Kafka producer publication**. A Booking and its pending Outbox event are committed atomically in PostgreSQL; a scheduled relay asynchronously publishes `BookingCreatedIntegrationEvent` to Kafka, waits for acknowledgment via `CompletableFuture<Void>`, and marks the record `PUBLISHED`. Local persistence tests and manual Kafka publication were verified. **No Kafka consumer is implemented yet.** See [Kafka and Transactional Outbox](docs/KAFKA.md) and [ROADMAP](docs/ROADMAP.md). Previous dated milestone descriptions below are historical snapshots.

A full-stack event management and booking platform built with **Java 21
and Spring Boot**, with React planned for the frontend.

The project is designed as a practical **Senior Java portfolio
project**, with a strong focus on reactive Java development,
Domain-Driven Design (DDD), Hexagonal Architecture and modern full-stack
development.

The application is being developed incrementally. Technologies and
architectural patterns are introduced when they are needed, and the
documentation is updated only after they have been studied and
implemented.

> **Architecture update — 15 October 2026:** EventHub is deliberately **evolving from a reactive monolith to a mixed-paradigm microservices learning project**. The Booking capability is being **extracted and redesigned** as an independent **imperative Spring MVC service**, rather than retained as a second active Booking implementation. Historical WebFlux/R2DBC Booking material remains in the documentation as evidence of earlier work, **not as a parallel current service**. This is a conscious portfolio/learning decision to practise both paradigms; it is **not presented as an optimal production architecture**. The independent Booking service now has JPA/PostgreSQL persistence, Flyway migrations, Testcontainers integration tests and a transactional use-case decorator; a first synchronous REST integration for Event availability is implemented and tested; a basic synchronous capacity-reservation/compensation path is implemented and unit/HTTP-mock-tested, but five basic manual two-service E2E scenarios passed on 17 October 2026; automated E2E and robust distributed recovery remain pending.

## Current components and ownership

| Component | Programming model | Persistence | Verified/documented capabilities |
| --- | --- | --- | --- |
| Event module (original EventHub application) | WebFlux / Reactor | PostgreSQL via R2DBC | Event API, event capacity, reactive transactions and optimistic locking |
| Booking module (historical, being replaced by extraction) | WebFlux / Reactor | PostgreSQL via R2DBC (historical) | `CustomerId` + places, `POST /bookings`, Event-capacity orchestration, reactive domain events |
| **`booking-service` (replacement under development)** | **Spring MVC / imperative** | **Spring Data JPA / PostgreSQL 17; Flyway V1/V2** | `UserId`, PENDING/CONFIRMED/CANCELLED, API-first HTTP (201/409), real JPA/HTTP integration tests, transactional rollback, active-booking uniqueness and concurrency checks |

The extracted service's build/deployment integration with the original application has **not been verified from the four documentation files alone**. Separate repository/module boundaries and exact startup commands should be confirmed in source before presenting them as runnable.

### Architecture decision — deliberate mixed reactive/imperative portfolio

**Motivation:** develop both non-blocking/reactive and synchronous/imperative backend expertise within one coherent EventHub portfolio. The Event side remains the reactive learning track (WebFlux, Reactor, R2DBC); Booking is moving out of the original reactive monolith into its own **Spring MVC** application, with **JPA/PostgreSQL implemented and integration-tested**.

**Migration interpretation:** this is an **evolution/replacement of the Booking capability**, not a deliberate long-term duplication of active Booking services. The old reactive Booking implementation, coordination, concurrency and event-flow examples are retained **as historical learning milestones**. No production migration, deletion of old source, successful end-to-end inter-service integration, or independent deployment is asserted without repository verification.

**Trade-off:** mixing stacks introduces extra maintenance, operations and integration complexity, and would not automatically be the preferred architecture in a real production system. Here the educational/portfolio value is the explicit reason for the choice. Both approaches retain DDD and hexagonal boundaries.

### New independent Booking Service — API-first workflow

```text
POST /api/bookings                       → 201 Created
POST /api/bookings (missing eventId)     → 400 Bad Request
POST /api/bookings (active duplicate)    → 409 Conflict
GET  /api/bookings/{id}                  → 200 OK / 404 Not Found
```

`BookingController` implements OpenAPI-generated `BookingApi`, converts the request's IDs into domain Value Objects, calls `BookingUseCase` and maps `Booking` to a generated `BookingResponse`. Errors use the generated RFC 9457 `ProblemDetail` and `application/problem+json`. `BookingService` uses the `BookingRepository` port backed by `JpaBookingRepositoryAdapter` (the in-memory adapter remains as a historical/test implementation); active duplicates are rejected by checking matching `(eventId, userId)` where status is not `CANCELLED`.

**Concurrency approach:** `existsActiveBooking()` improves normal-path feedback but cannot prevent a check-then-insert race. PostgreSQL now enforces active-booking uniqueness through Flyway V2 (`uq_bookings_active_event_user`), and the JPA adapter translates only this constraint failure into `BookingAlreadyExistsException`. A two-thread integration test passed; it coordinates starting the requests but does **not** force both initial existence checks to return false. Cross-service workflows remain pending.

### Booking persistence and transaction milestone — 15 October 2026

- Independent `booking-service/` application: Spring MVC / imperative HTTP adapter, Spring Data JPA / Hibernate, and PostgreSQL 17.
- Flyway migration `V1__create_bookings_table.sql` creates `bookings(booking_id, event_id, user_id, status)`; schema managed by migrations, not Hibernate auto-DDL.
- `BookingJpaEntity` maps persisted UUIDs and status to and from the immutable `Booking` domain model using `Booking.rehydrate(...)`. `JpaBookingRepositoryAdapter` implements the existing `BookingRepository` output port.
- JPA integration tests verified insert/read, active-booking lookup, exclusion of cancelled bookings, and update `PENDING → CONFIRMED` using `flush()`/`clear()`.
- PostgreSQL Testcontainers with `@ServiceConnection` runs integration tests on an isolated database; Flyway initializes its schema.
- The Spring-managed `TransactionalBookingUseCase` decorator implements `BookingUseCase` and declares `@Transactional` boundaries without adding Spring dependencies to `BookingService`. `BookingConfiguration` wraps the plain-Java service in this decorator.
- Integration tests **without test-level `@Transactional`** verified booking persistence and rollback after a deliberately failing delegate. A negative control removing the decorator's `@Transactional` caused the expected failure (the previously saved booking remained in PostgreSQL).
- Flyway `V2__unique_active_booking.sql` adds a partial unique index on `(event_id, user_id)` for statuses other than `CANCELLED`; tested rejection of two active bookings and successful rebooking after cancellation.
- `JpaBookingRepositoryAdapter.save()` now uses `saveAndFlush()` to surface database errors inside the adapter. The adapter inspects the Hibernate constraint name nested in Spring’s `DataIntegrityViolationException`, translates only `uq_bookings_active_event_user` to `BookingAlreadyExistsException` and propagates unrelated failures unchanged (Mockito test). `flush()` is not a commit.
- Two-thread `ConcurrentBookingIT` with `ExecutorService`/`CountDownLatch` passed: one creation succeeded and the other was rejected. This is a concurrent-start test, not a deterministic forcing of both existence reads to false.
- `TransactionalBookingUseCaseIT` verified persisted creation and the transitions `PENDING → CONFIRMED`, `PENDING → CANCELLED`, `CONFIRMED → CANCELLED`. `BookingHttpIT` verified real `POST /api/bookings` → `201`, and repeated POST → `409` with `BOOKING_ALREADY_EXISTS`, through MockMvc and Testcontainers, without mocks of application services.
- **Subsequent milestone:** a basic synchronous Event-capacity reservation and compensation flow has now been implemented (below). Still pending: live two-service end-to-end test, durable/idempotent distributed recovery, Kafka integration, deterministic race-barrier test and full deployment.

### Cross-service REST availability — 15 October 2026

Booking Service (Spring MVC, port 8081) synchronously queries Event Service (WebFlux, port 8080) through application output port `EventAvailabilityPort` and infrastructure `RestEventAvailabilityAdapter` using Spring `RestClient`. The client requests `GET /events/{id}` and considers an event available only if `status == PUBLISHED` and `available > 0`. A reduced `EventResponse` DTO reads only the fields needed by Booking. The call validates availability but **does not reserve Event capacity**, so distributed overselling protection is still pending.

**Historical flow before the compensation milestone:** `BookingService.createBooking` checked for an existing active booking, then called `EventAvailabilityPort`, and saved a new `PENDING` booking if available. The current flow now uses `CompensatingBookingUseCase` and `EventCapacityPort` (below). Errors are translated at infrastructure boundaries: a missing remote event becomes `EventNotFoundException` and HTTP `404 EVENT_NOT_FOUND`; an existing but unavailable event becomes `409 EVENT_NOT_AVAILABLE`; remote HTTP 5xx and connection/read failures become `EventServiceUnavailableException` and HTTP `503 EVENT_SERVICE_UNAVAILABLE`. Error bodies use generated RFC 9457 `ProblemDetail`. `RestClient` has a 2-second connection timeout and 3-second read timeout configured with `SimpleClientHttpRequestFactory`; expiration of those limits has **not** been independently verified with a timing test.

Testing includes `RestEventAvailabilityAdapterTest` with `MockRestServiceServer` for published/full/draft/cancelled events, extra JSON properties, 404, 500 and simulated I/O failure; `BookingServiceTest` with mocked ports; `BookingHttpIT` with real Spring MVC/PostgreSQL and mocked Event availability for 201/409/404/503; `ConcurrentBookingIT` and transaction integration tests with the external port mocked. A manual cross-service GET has also been exercised. Maven Failsafe now runs `*IT` tests during `verify`:

```powershell
cd booking-service
.\mvnw.cmd clean verify
```

Remaining: independently test actual timeout expiration; refine GET-availability Retry/Circuit Breaker/Bulkhead policies; decide whether remote 429/other 4xx need explicit mappings; verify the new Event capacity reserve/release and synchronous compensation path end to end; design reliable/idempotent distributed recovery. A reusable communication checklist is planned once those topics have been studied.

### Event availability resilience — 15–16 October 2026

The independent Booking Service protects its synchronous Event availability HTTP call through **Resilience4j Bulkhead → Circuit Breaker → Retry → RestClient**. All decorators implement the hexagonal `EventAvailabilityPort`; Booking application/domain code does not depend on Resilience4j.

- **HTTP:** connection timeout 2 s; read timeout 3 s (configured, expiry not timing-tested).
- **Retry:** maximum 3 total attempts, 500 ms between attempts, only for `EventServiceUnavailableException`.
- **Circuit Breaker:** 50% failure-rate threshold, count-based window of 10, minimum 10 calls, 10 s OPEN wait, 2 HALF_OPEN probes; remote 404 ignored.
- **Bulkhead:** semaphore with 5 concurrent operations and zero wait; rejected calls are translated from `BulkheadFullException` to `EventServiceUnavailableException` (HTTP 503).
- **Error handling:** Circuit Breaker OPEN or Bulkhead full are translated to `EventServiceUnavailableException` and returned as HTTP 503.
- **Verified:** isolated Retry/Circuit Breaker tests, a latch-based Bulkhead concurrency test and Spring outer-bean wiring; automatic timer transition, global latency budget and durable/idempotent distributed capacity coordination remains pending.

**Full explanation, configuration, architecture diagram, test cases and known limitations:** [Resilience — Timeout, Retry, Circuit Breaker & Bulkhead](docs/RESILIENCE.md).

### Event capacity reservation and synchronous compensation — 16 October 2026

The **current** `POST /api/bookings` flow uses `CompensatingBookingUseCase`, not the earlier `EventAvailabilityPort.isAvailable()` check. Booking Service remains **imperative** (Spring MVC, RestClient, JPA/PostgreSQL), while Event Service implements the capacity API with **WebFlux/Reactor/R2DBC**.

```text
POST /api/bookings  [Booking Service :8081]
  → CompensatingBookingUseCase
    → EventCapacityPort.reservePlaces(eventId, 1)
      → POST /events/{id}/reserve/1/places  [Event Service :8080]
    → TransactionalBookingUseCase (@Transactional, separate Spring bean)
      → BookingService → JPA / PostgreSQL
    → if creation fails: EventCapacityPort.releasePlaces(eventId, 1)
      → POST /events/{id}/release/1/places  [Event Service :8080]
```

Event's `Capacity` value object raises `InsufficientCapacityException` or `ExcessiveCapacityException` to protect its invariants; the Event HTTP advice maps those domain violations to **409 Conflict**. For `reservePlaces`, the Booking REST adapter maps HTTP 409 to `EventNotAvailableException`, 404 to `EventNotFoundException`, and HTTP 5xx/transport failures to `EventServiceUnavailableException`. Release HTTP 409 currently propagates as a raw client conflict; it does **not** mean insufficient available capacity. Mutating POSTs have **no automatic Retry**. The previous `Bulkhead → Circuit Breaker → Retry → REST` implementation remains documented for the **read-only GET availability** path, not for current booking creation.

**Verified:** Event insufficient-capacity HTTP test; eight `RestEventCapacityAdapterTest` HTTP-mock tests; three `CompensatingBookingUseCaseTest` unit tests. Existing Booking HTTP/JPA integration tests were adapted to mock `EventCapacityPort`. **Manual E2E verified (17 October 2026):** Booking created (HTTP 201) with Event capacity 20→19; duplicate Booking rejected (409) with Event final capacity unchanged at 19; missing Event (404); exhausted capacity (409); Event Service stopped (503). A controlled post-reservation persistence failure and automatic E2E tests are still pending. This is **best-effort synchronous compensation**, not a distributed transaction: ambiguous timeouts, idempotency, durable recovery and capacity release on booking cancellation remain outstanding.

### Manual E2E evidence — 17 October 2026

The two running services were tested directly over HTTP without mocked inter-service calls. Observed results: Booking `201 PENDING` and Event `available` from 20 to 19; duplicate Booking `409 BOOKING_ALREADY_EXISTS` with Event capacity still 19 afterward; missing Event `404 EVENT_NOT_FOUND`; exhausted Event `409 EVENT_NOT_AVAILABLE` while availability remained 0; stopped Event Service `503 EVENT_SERVICE_UNAVAILABLE`. Full evidence, IDs, scope and remaining verification gaps are documented in [`docs/MICROSERVICES_COMMUNICATION.md`](docs/MICROSERVICES_COMMUNICATION.md).

**Next:** Apache Kafka fundamentals and a first `BookingCreated` notification integration. The existing synchronous REST capacity-reservation flow remains unchanged.

### Documentation map

- [`docs/ROADMAP.md`](docs/ROADMAP.md): complete project progress; new independent service in section 26.
- [`docs/DDD.md`](docs/DDD.md): domain model, value objects, aggregates and domain events.
- [`docs/HEXAGONAL.md`](docs/HEXAGONAL.md): application ports, adapters and Spring wiring.
- [`docs/PERSISTENCE_CONSISTENCY.md`](docs/PERSISTENCE_CONSISTENCY.md): persistence, concurrency, transactions and distributed compensation.
- [`docs/REACTIVE.md`](docs/REACTIVE.md): original EventHub reactive learning and code, distinguishing Reactor retry from imperative Resilience4j.
- [`docs/RESILIENCE.md`](docs/RESILIENCE.md): synchronous inter-service timeout, Retry, Circuit Breaker, Bulkhead, test coverage, and pending work.
- [`docs/MICROSERVICES_COMMUNICATION.md`](docs/MICROSERVICES_COMMUNICATION.md): reusable service-to-service communication checklist, practical tests, expected evidence, and EventHub progress.


------------------------------------------------------------------------

# 🚀 Tech Stack

## Backend

-   Java 21
-   Spring Boot 4.1
-   Maven
-   JUnit 5
-   Project Reactor
-   Reactor Test / StepVerifier
-   Spring WebFlux
-   WebTestClient

## Architecture

Currently introduced:

-   Domain-Driven Design (DDD)
-   Rich Domain Model
-   Entity
-   Value Objects
-   Aggregate Root
-   Domain invariants
-   Hexagonal Architecture
-   Ports and Adapters
-   Application layer / use cases
-   Inbound and outbound ports
-   Dependency Inversion
-   Dependency Injection / Spring IoC wiring
-   Input and output adapters
-   Reactive HTTP input adapter
-   Reactive Programming fundamentals
-   Reactive Streams concepts
-   Mono / Flux
-   Reactive error handling
-   Cold and hot publishers
-   Reactor schedulers and threading fundamentals
-   Blocking vs non-blocking execution
-   Reactive application/repository ports
-   HTTP request/response DTO mapping
-   WebFlux exception handling
-   Jakarta Bean Validation / request validation
-   Booking Aggregate and Booking application flow
-   Cross-module coordination through application ports
-   Publish Event application use case and HTTP endpoint
-   Booking WebFlux HTTP input adapter
-   Focused controller testing with Mockito and WebTestClient
-   Concurrent booking race-condition testing
-   Optimistic locking / Event versioning
-   Bounded recovery after concurrent Event updates
-   Booking query by Event through an application use case
-   Reactive transaction / atomic Booking creation
-   Domain Events and internal reactive Domain Event publication
-   Domain Events vs Integration Events
-   Domain Event handlers and generic dispatcher
-   Integration Event contracts and publisher output port

Planned in the project roadmap: - Modular Monolith - Event-Driven
Architecture

## Frontend

Planned:

-   TypeScript
-   React

## Persistence

Currently introduced:

-   PostgreSQL 17
-   Spring Data R2DBC
-   PostgreSQL R2DBC driver
-   Spring Data JPA / Hibernate (independent Booking service)
-   Flyway schema migrations (independent Booking service)
-   ReactiveCrudRepository
-   Domain ↔ persistence mapping
-   SQL schema initialization
-   Optimistic locking with persisted Event version

## Infrastructure

Currently introduced:

-   Docker Compose for local PostgreSQL
-   Testcontainers + PostgreSQL 17 (`booking-service` integration tests)

Planned:

-   Backend/frontend Docker images
-   Apache Kafka
-   Kubernetes
-   Helm

## CI/CD

Planned:

-   GitHub Actions
-   GitHub Container Registry (GHCR)

## Development Tools

-   Maven Wrapper
-   Git / GitHub
-   IntelliJ IDEA
-   JUnit 5

------------------------------------------------------------------------

# 📚 Detailed Documentation

Detailed learning material is kept in separate documents and is updated
as each topic is actually studied.

| Topic | Documentation |
| --- | --- |
| Project progress | [ROADMAP.md](docs/ROADMAP.md) |
| Domain-Driven Design | [DDD.md](docs/DDD.md) |
| Hexagonal Architecture | [HEXAGONAL.md](docs/HEXAGONAL.md) |
| Persistence and Consistency | [PERSISTENCE_CONSISTENCY.md](docs/PERSISTENCE_CONSISTENCY.md) |

| Reactive Programming | [REACTIVE.md](docs/REACTIVE.md) |
| Inter-service resilience (Timeout, Retry, Circuit Breaker) | [RESILIENCE.md](docs/RESILIENCE.md) |
| Microservices communication: practical checklist and testing | [MICROSERVICES_COMMUNICATION.md](docs/MICROSERVICES_COMMUNICATION.md) |

Additional documentation will be created when the corresponding topics
are reached in the course.

------------------------------------------------------------------------

# 🗺️ Current Position

``` text
Project initialization                  ✅
        ↓
DDD fundamentals / Event Aggregate      ✅
        ↓
Hexagonal Architecture                  🚧 IN PROGRESS
        ↓
Application / Ports / Adapters          ✅
        ↓
Reactive Programming Fundamentals       ✅
        ↓
Spring WebFlux                          🚧 IN PROGRESS
        ↓
Reactive HTTP input adapter             ✅
        ↓
POST /events                            ✅
GET  /events                            ✅
GET  /events/{id}                       ✅
        ↓
HTTP error handling                     ✅
        ↓
Request validation                      ✅
        ↓
Booking domain / application flow       ✅
        ↓
Publish Event API                       ✅
        ↓
Booking HTTP API                        ✅
        ↓
Event R2DBC / PostgreSQL                ✅
        ↓
Booking R2DBC / PostgreSQL              ✅
        ↓
Concurrency / optimistic locking        ✅
        ↓
Reactive transaction / atomicity        ✅
        ↓
Domain Events                           ✅
        ↓
Domain Events vs Integration Events     ✅
        ↓
Kafka fundamentals                      ⏸ DEFERRED (original reactive roadmap)
        ↓
Booking Service JPA / PostgreSQL        ✅
        ↓
Flyway / JPA integration tests          ✅
        ↓
Transactional decorator / rollback      ✅
        ↓
Concurrent duplicate protection         ✅
        ↓
HTTP + use-case integration tests       ✅
        ↓
Cross-service Event availability REST   ✅
        ↓
Retry + Circuit Breaker + Bulkhead      ✅
        ↓
Distributed capacity coordination    🚧 NEXT
```

For the complete project plan, check [ROADMAP.md](docs/ROADMAP.md).

------------------------------------------------------------------------

# 🏗️ Technical Picture

The original EventHub application contains framework-independent Event and Booking domain
models, reactive application ports, R2DBC/PostgreSQL output adapters for
Event and Booking, Spring WebFlux HTTP input adapters for Event and
Booking, and Spring IoC configuration for dependency wiring. Booking
creation coordinates Event capacity through an Event application input
port rather than accessing Event persistence directly. Event publication
is also exposed as an explicit application use case.

``` text
HTTP Client
    ↓
EventController                              INPUT ADAPTER
    ↓
CreateEventUseCase / EventQueryUseCase       INPUT PORTS
    ↓
CreateEventService / EventQueryService       APPLICATION
    ↓
Event                                        DOMAIN
    ↓
EventRepository                              OUTPUT PORT
    ↓
R2dbcEventRepositoryAdapter                  OUTPUT ADAPTER
    ↓
SpringDataEventRepository
    ↓
PostgreSQL 17
    ↓
Mono<Event> / Flux<Event>
    ↓
EventResponse
    ↓
HTTP JSON

Spring EventConfiguration                    IoC / WIRING
```

Current HTTP API:

``` text
POST /events                       → 201 Created
GET  /events                        → 200 OK
GET  /events/{existing-id}          → 200 OK
GET  /events/{missing-valid-id}     → 404 Not Found
GET  /events/{invalid-id-format}    → 400 Bad Request
POST /events/{id}/publish           → 200 OK
POST /bookings                      → 201 Created
```

Current source structure:

``` text
eventhub-reactive
│
├── src/main/java/com/rubenmarin/eventhub/event/
│   ├── domain/model/
│   ├── application/
│   │   ├── port/in/
│   │   ├── port/out/
│   │   └── service/
│   └── infrastructure/
│       ├── adapter/in/web/
│       ├── adapter/out/persistence/
│       │   ├── InMemoryEventRepository.java
│       │   └── r2dbc/
│       │       ├── EventEntity.java
│       │       ├── SpringDataEventRepository.java
│       │       └── R2dbcEventRepositoryAdapter.java
│       └── config/
│
├── src/test/java/com/rubenmarin/eventhub/event/
│   ├── domain/model/
│   ├── application/service/
│   └── infrastructure/
│
├── src/test/java/com/rubenmarin/eventhub/reactive/
│   └── ReactorBasicsTest.java
│
└── docs/
    ├── DDD.md
    ├── HEXAGONAL.md
    ├── PERSISTENCE_CONSISTENCY.md
    ├── REACTIVE.md
    └── ROADMAP.md
```

The domain remains independent of Spring, Reactor and PostgreSQL.
Spring-specific HTTP, R2DBC persistence adapters and dependency wiring
live in infrastructure. `Event.create()` creates new Aggregates, while
`Event.rehydrate()` reconstructs persisted Aggregates without leaking
persistence annotations into the domain.

------------------------------------------------------------------------

# 🏛️ Current Domain Model

EventHub currently contains the Event aggregate and the first Booking
aggregate implementation.

``` text
                  Event
             Aggregate Root
                  │
       ┌──────────┼──────────┐
       │          │          │
       ▼          ▼          ▼
    EventId    Capacity    Money
       VO         VO         VO
```

The current Event model also contains:

``` text
Event
│
├── EventId
├── EventName
├── Capacity
├── Money
├── EventStatus
│
├── publish()
├── cancel()
├── reservePlaces()
└── releasePlaces()
```

The domain protects its own business rules rather than exposing
unrestricted state modification.

Detailed notes and examples are available in [DDD.md](docs/DDD.md).

------------------------------------------------------------------------

# 🗄️ Reactive Event Persistence

Event persistence is now backed by PostgreSQL 17 through Spring Data
R2DBC. PostgreSQL runs locally through Docker Compose, and `schema.sql`
initializes the `events` table.

The application-owned `EventRepository` distinguishes persistence intent
with `create(Event)` and `update(Event)`. The R2DBC adapter maps the
domain Aggregate to `EventEntity` and maps database results back through
`Event.rehydrate(...)`.

Because Event IDs are generated in the domain before persistence,
`EventEntity` implements `Persistable<UUID>` and uses a transient
`isNew` flag:

``` text
create(event) → isNew=true  → INSERT
update(event) → isNew=false → UPDATE
```

Both paths have been manually verified against PostgreSQL: newly created
Events are persisted as `DRAFT`, and publication updates the same row to
`PUBLISHED`.

Reactive transaction management is now implemented for Booking creation.
For the **original reactive Event module**, migrations and Testcontainers remain pending; these are implemented separately for the **imperative `booking-service`**.

------------------------------------------------------------------------

# 🗄️ Original Reactive Booking Module — Persistence

Booking persistence is now also backed by PostgreSQL through Spring Data
R2DBC.

The existing Booking flow keeps the module boundary already introduced:
Booking coordinates Event capacity through `ReserveEventPlacesUseCase`,
while Event persistence remains owned by the Event module. After
capacity is reserved, the Booking is persisted through its own
repository adapter.

``` text
POST /bookings
    ↓
CreateBookingService
    ↓
ReserveEventPlacesUseCase
    ↓
EventRepository.update(...)
    ↓
PostgreSQL: Event capacity updated
    ↓
BookingRepository
    ↓
R2DBC / PostgreSQL: Booking inserted
```

The complete flow was manually verified against PostgreSQL: reserving 3
places reduced Event availability from `20` to `17`, and the
corresponding Booking was persisted.

The Event capacity update and Booking insert are now coordinated as one
reactive transaction. If Booking persistence fails after the Event
update, the Event update is rolled back so the database does not retain
a partial business state.

------------------------------------------------------------------------

# 🔒 Original Reactive Booking Module — Optimistic Locking

Concurrent booking operations can read the same Event capacity before
either update is persisted. The project now reproduces this race
condition and uses optimistic locking on the persisted Event version to
prevent a stale update from silently overwriting the winner.

``` text
Booking A ──► read version N ──► update succeeds ──► version N + 1
Booking B ──► read version N ──► stale update ──► optimistic locking failure
```

This protects Event capacity against the lost-update/overselling
scenario studied so far. A focused concurrency test verifies the
behaviour and the full test suite is green.

The booking that loses the version race is now recovered by re-reading
the latest Event state and re-evaluating the reservation once.
Infrastructure translates the Spring Data optimistic-locking failure
into the application-level `ConcurrentUpdateException`, keeping the
application independent of Spring Data.

When two concurrent Bookings request 2 places each from an Event with
capacity 3, the integration test now verifies the final persisted
business state:

``` text
Bookings persisted        = 1
Event available places    = 1
```

The test queries Bookings by Event through `BookingQueryUseCase` and
verifies the persisted Event through `EventQueryUseCase`.

Reactive transaction/atomicity across Event and Booking persistence is
now implemented and tested. A failure injected after the Event update
verifies that the capacity change is rolled back.

------------------------------------------------------------------------

# 🧪 Testing

The project now contains pure domain unit tests, application-service
tests using a fake reactive repository port, output-adapter tests,
Spring wiring tests, focused Reactor learning tests and WebFlux HTTP
integration tests with `WebTestClient`.

Domain and application unit tests require no Spring ApplicationContext,
database, Docker or HTTP server. The dedicated configuration test
intentionally starts a Spring ApplicationContext to validate IoC wiring.

Current tested behaviour includes:

-   valid and invalid Capacity creation
-   reserving places
-   releasing places
-   immutable Capacity behaviour
-   insufficient-capacity rejection
-   Event creation as DRAFT
-   Event publication
-   rejection of reservations for DRAFT Events
-   reservation for PUBLISHED Events
-   rejection of reservations exceeding available capacity
-   Money creation and validation
-   Mono and Flux creation and signals
-   lazy execution and subscription
-   map, filter, flatMap, concatMap, switchIfEmpty and zip
-   reactive error propagation, retry and timeout
-   cold and hot publisher behaviour
-   StepVerifier-based reactive testing
-   subscribeOn and publishOn thread switching
-   parallel and boundedElastic scheduler fundamentals
-   reactive Event creation through `POST /events`
-   Event listing through `GET /events`
-   Event lookup through `GET /events/{id}` using the UUID returned by
    the create request
-   missing Event lookup returning `404 Not Found`
-   malformed Event ID returning `400 Bad Request`
-   WebFlux exception translation with `@RestControllerAdvice` and
    `@ExceptionHandler`
-   JSON request/response media types and `415 Unsupported Media Type`
    behaviour
-   request validation with Bean Validation and `400 Bad Request`
    validation responses
-   Booking creation and cancellation domain rules
-   parameterized Booking invariant tests
-   reactive Event-capacity reservation through
    `ReserveEventPlacesUseCase`
-   Booking creation with `CreateBookingService` and `StepVerifier`
-   insufficient Event capacity propagated as a reactive error
-   isolated application-test setup with JUnit 5 `@BeforeEach`
-   Publish Event service success and missing-Event paths
-   Event publication through `POST /events/{id}/publish`
-   Event availability exposed separately from total capacity
-   Booking creation through `POST /bookings`
-   isolated `BookingController` testing with Mockito + `WebTestClient`
-   concurrent Booking race condition and optimistic-locking failure
-   bounded recovery after an optimistic-lock conflict
-   final concurrent-booking state: one persisted Booking and one
    remaining Event place
-   Booking lookup by Event and reactive `count()` verification
-   reactive transaction rollback when Booking persistence fails after
    Event capacity update
-   BookingCreated Domain Event registration and internal reactive
    publication
-   Domain Event dispatch to the matching BookingCreated handler
-   BookingCreated Domain Event → BookingCreatedIntegrationEvent mapping

The independent Booking service additionally has JPA repository integration tests, partial-unique-index and exception-translation tests, a concurrent-start test, full HTTP integration tests, PostgreSQL Testcontainers and transactional-decorator tests (commit visibility, exception-triggered rollback and negative control without `@Transactional`). Maven Failsafe is configured to run the Booking integration-test classes ending in `IT` during `verify`. To run a specific subset of tests manually from `booking-service/`:

```powershell
.\mvnw.cmd test "-Dtest=JpaBookingRepositoryAdapterIT,TransactionalBookingUseCaseIT,TransactionalBookingRollbackIT,ConcurrentBookingIT,BookingHttpIT"
```

The original EventHub application build was verified with (execute from its project root):

``` powershell
.\mvnw.cmd clean test
```

------------------------------------------------------------------------

# 🎯 Project Goal

The goal is not only to create a working event management application.

The project is intended to refresh and expand modern Java development
skills through a practical application, progressively covering the
topics defined in the project roadmap.

The project places particular emphasis on:

-   Java 21
-   Spring Boot
-   Domain-Driven Design
-   Hexagonal Architecture
-   Reactive Java
-   Spring WebFlux
-   Project Reactor
-   R2DBC
-   PostgreSQL
-   Event-Driven Architecture
-   React
-   TypeScript
-   Testing
-   Docker
-   CI/CD
-   Kubernetes
-   Helm
-   Observability
-   System Design

------------------------------------------------------------------------

# 👨‍💻 Author

**Rubén Marín**

Backend Java Developer

Current technologies, architecture patterns and practices already
explored in this project:

`Java 21` · `Spring Boot 4.1` · `Maven` · `JUnit 5` · `DDD` · `Entity` ·
`Value Objects` · `Aggregate Root` · `Domain Invariants` ·
`Rich Domain Model` · `Hexagonal Architecture` · `Ports & Adapters` ·
`Dependency Inversion` · `Dependency Injection` · `Spring IoC` ·
`Project Reactor` · `Mono` · `Flux` · `StepVerifier` ·
`Reactor Schedulers` · `Spring WebFlux` · `WebTestClient` ·
`Reactive HTTP` · `Spring Data R2DBC` · `PostgreSQL 17` ·
`Docker Compose`
