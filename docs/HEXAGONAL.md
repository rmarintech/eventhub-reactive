# Hexagonal Architecture — EventHub

Application ports, use cases, inbound/outbound adapters, dependency inversion, Spring wiring and HTTP boundaries. Domain modeling belongs in [DDD.md](DDD.md); transactions and distributed consistency belong in [PERSISTENCE_CONSISTENCY.md](PERSISTENCE_CONSISTENCY.md).

---

## DDD and Hexagonal Architecture

The project now combines the domain model with a Hexagonal Architecture
boundary.

The distinction studied so far is:

``` text
DDD
  ↓
models the business and its rules

Hexagonal Architecture
  ↓
isolates the application and domain from external technical details
```

The Event domain remains pure Java. The application layer coordinates
use cases around that domain, while ports define the boundaries through
which external adapters interact with the application.

Current dependency direction:

``` text
Infrastructure
      ↓
Application
      ↓
Domain
```

The domain does not depend on the application or infrastructure layers.

------------------------------------------------------------------------

---

## Reactive Application Layer and Create Event Use Case

The application boundary is now reactive while the domain remains
synchronous and framework-independent.

The inbound command currently contains the HTTP-independent values
required to create an Event, including its currency.

The inbound port returns a reactive result:

``` java
public interface CreateEventUseCase {
    Mono<Event> createEvent(CreateEventCommand command);
}
```

`CreateEventService` creates the domain Aggregate synchronously and
delegates persistence through the reactive repository port:

``` text
CreateEventCommand
        ↓
CreateEventUseCase
        ↑
CreateEventService
        ↓
Event.create(...)          synchronous domain logic
        ↓
EventRepository.create(...)
        ↓
Mono<Event>
```

Reactive types are used at the application boundary and infrastructure
interaction; `Event`, `Capacity`, `Money` and the other domain types
still contain no `Mono` or `Flux`.

------------------------------------------------------------------------

---

## Reactive Outbound Port

The repository port now expresses reactive persistence/query contracts:

``` java
public interface EventRepository {
    Mono<Event> create(Event event);
    Mono<Event> update(Event event);
    Mono<Event> findById(EventId id);
    Flux<Event> findAll();
}
```

The cardinality is explicit:

``` text
create(...)    → 0..1 result → Mono<Event>
update(...)    → 0..1 result → Mono<Event>
findById(...)  → 0..1 result → Mono<Event>
findAll()      → 0..N results → Flux<Event>
```

A missing Event is represented by an empty `Mono`, rather than by
`Mono<Optional<Event>>`.

------------------------------------------------------------------------

---

## Reactive Output Adapter and Lazy Execution

The in-memory output adapter was used first to study lazy reactive
execution. It remains available as a plain Java test adapter, while
production Event persistence now uses the R2DBC adapter described later
in this document.

`create()` and `update()` use `Mono.fromSupplier(...)` because the
subscription lazily produces a value while performing the in-memory
save.

`findById()` and `findAll()` use deferred publisher creation so the
current contents of the map are inspected at subscription time.

The distinction studied is:

``` text
fromSupplier → lazy VALUE

defer        → lazy PUBLISHER
```

The adapter remains an infrastructure implementation of the
application-owned `EventRepository` port.

------------------------------------------------------------------------

---

## Query Input Port and Application Service

Read operations are grouped in one query-oriented inbound port rather
than creating one interface per simple query:

``` java
public interface EventQueryUseCase {
    Mono<Event> findById(EventId id);
    Flux<Event> findAll();
}
```

`EventQueryService` implements this port and delegates to
`EventRepository`.

``` text
EventQueryUseCase
        ↑
EventQueryService
        ↓
EventRepository
   ├── findById(...)
   └── findAll()
```

This is an organization of application responsibilities; CQRS has not
been introduced.

------------------------------------------------------------------------

---

## Dependency Inversion, Injection and Spring IoC

The application services depend on `EventRepository`, an abstraction
owned by the application boundary. Infrastructure implements that
abstraction.

Both application services receive the repository through constructor
injection and remain free of Spring annotations.

Spring wiring lives in infrastructure:

``` java
@Configuration
public class EventConfiguration {

    @Bean
    public CreateEventUseCase createEventUseCase(EventRepository eventRepository) {
        return new CreateEventService(eventRepository);
    }

    @Bean
    public EventQueryUseCase eventQueryUseCase(EventRepository eventRepository) {
        return new EventQueryService(eventRepository);
    }
}
```

The distinction studied remains:

``` text
Dependency Inversion
    → depend on an abstraction / port

Dependency Injection
    → receive the dependency from outside

Inversion of Control
    → Spring controls object creation and wiring
```

------------------------------------------------------------------------

---

## HTTP Input Adapter with Spring WebFlux

The HTTP input adapter has now been implemented with Spring WebFlux.

The current controller exposes:

``` text
POST /events       → create Event
GET  /events       → list Events
GET  /events/{id}  → find Event by id
```

HTTP-specific DTOs are kept in infrastructure:

``` text
HTTP JSON
    ↓
CreateEventRequest          infrastructure input DTO
    ↓
CreateEventCommand          application command
    ↓
CreateEventUseCase
    ↓
Event                       domain Aggregate
    ↓ map(...)
EventResponse               infrastructure output DTO
    ↓
HTTP JSON
```

This prevents the HTTP contract from becoming the domain model.

The controller uses `map()` for the synchronous transformation from
`Event` to `EventResponse`; it does not call `subscribe()` manually.
WebFlux performs the subscription at the HTTP boundary.

------------------------------------------------------------------------

---

## HTTP Contract Studied So Far

The create endpoint explicitly consumes and produces JSON and returns
`201 Created`:

``` text
POST /events

Request
Content-Type: application/json
Accept: application/json

Response
201 Created
Content-Type: application/json
```

The distinction studied is:

``` text
Content-Type → format of the body being sent
Accept       → format the client wants to receive
consumes     → formats accepted by the endpoint
produces     → formats produced by the endpoint
```

Sending an unsupported request media type was tested and WebFlux
returned `415 Unsupported Media Type` before entering the controller.

The GET endpoints currently cover the successful path:

``` text
GET /events       → 200 OK + JSON collection
GET /events/{id}  → 200 OK + JSON EventResponse when the Event exists
```

The unsuccessful find-by-id paths have now also been implemented and
tested:

``` text
GET /events/{valid-existing-id}     → 200 OK
GET /events/{valid-missing-id}      → 404 Not Found
GET /events/{invalid-id-format}     → 400 Bad Request
```

------------------------------------------------------------------------

---

## Reactive HTTP and Repository Flow

The current vertical slice is:

``` text
HTTP request
    ↓
EventController                         INPUT ADAPTER
    ↓
CreateEventUseCase / EventQueryUseCase  INPUT PORTS
    ↓
Application Service
    ↓
Event                                   DOMAIN
    ↓
EventRepository                         OUTPUT PORT
    ↓
EventRepository implementation           OUTPUT ADAPTER
    ↓
Mono<Event> / Flux<Event>
    ↓
map(Event → EventResponse)
    ↓
Mono<EventResponse> / Flux<EventResponse>
    ↓
WebFlux subscribes and serializes JSON
```

The domain itself remains synchronous pure Java.

------------------------------------------------------------------------

---

## Tests at the Current Architecture Boundary

The project now tests the architecture at several levels:

``` text
DOMAIN
  → pure business-rule unit tests

APPLICATION
  → use-case tests with a fake EventRepository

OUTPUT ADAPTER
  → in-memory repository behaviour

SPRING / HTTP INPUT ADAPTER
  → WebTestClient integration tests
```

The fake repository was updated to preserve the semantics of the
reactive port: lazy save/query execution, empty results when
appropriate, and ID-aware `findById()` behaviour.

`WebTestClient` tests currently verify the successful create/list/find
flows. The find-by-id test creates an Event through `POST /events`,
captures the generated UUID from the response, and then retrieves that
same resource through `GET /events/{id}`.

------------------------------------------------------------------------

---

## Current Architecture Structure

``` text
com.rubenmarin.eventhub.event
│
├── domain
│   └── model
│       ├── Event.java
│       ├── EventId.java
│       ├── EventName.java
│       ├── EventStatus.java
│       ├── Capacity.java
│       └── Money.java
│
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── CreateEventCommand.java
│   │   │   ├── CreateEventUseCase.java
│   │   │   └── EventQueryUseCase.java
│   │   └── out
│   │       └── EventRepository.java
│   └── service
│       ├── CreateEventService.java
│       └── EventQueryService.java
│
└── infrastructure
    ├── adapter
    │   ├── in/web
    │   │   ├── EventController.java
    │   │   ├── CreateEventRequest.java
    │   │   └── EventResponse.java
    │   └── out/persistence
    │       └── InMemoryEventRepository.java
    └── config
        └── EventConfiguration.java
```

---

## HTTP Error Handling

A repository lookup that does not find an Event is represented by
`Mono.empty()`. The repository does not decide HTTP semantics.

`EventQueryService` translates that absence into an application-specific
error using the Reactor operator already studied:

``` text
EventRepository.findById(...)
        ↓
Mono.empty()
        ↓
switchIfEmpty(Mono.error(...))
        ↓
EventNotFoundException
```

`EventNotFoundException` belongs to the application layer because the
application decides that a missing Event is an error for this use case.
It contains no HTTP knowledge.

The WebFlux input adapter translates application/web exceptions into
HTTP responses through `@RestControllerAdvice` and `@ExceptionHandler`:

``` text
EventNotFoundException
        ↓
GlobalExceptionHandler
        ↓
404 Not Found
```

A malformed Event identifier is different. The controller receives a
`String` from HTTP and must translate it into an `EventId`. The
conversion is isolated in `parseEventId(String id)`.

``` text
HTTP String
    ↓
parseEventId(...)
    ↓
UUID.fromString(...)
    ├── valid   → EventId
    └── invalid → InvalidEventIdException
                     ↓
               GlobalExceptionHandler
                     ↓
               400 Bad Request
```

`InvalidEventIdException` is kept in the web input adapter because the
failure currently belongs to translation of HTTP input into the type
required by the application.

A generic `@ExceptionHandler(IllegalArgumentException.class)` was
deliberately avoided after testing it, because domain code also uses
`IllegalArgumentException` for invariants. Mapping every such exception
to HTTP 400 could incorrectly classify an unrelated application or
programming error as a client error.

The resulting distinction is:

``` text
valid EventId + Event exists       → 200 OK
valid EventId + Event missing      → 404 Not Found
invalid EventId representation     → 400 Bad Request
```

------------------------------------------------------------------------

---

## HTTP Integration Test Isolation

Earlier WebFlux integration tests exposed the lifecycle of the in-memory
repository when it was the Spring production adapter. The production
Event repository has since been replaced by the R2DBC adapter;
`InMemoryEventRepository` is now a plain Java adapter used by tests that
instantiate it directly.

The list test was changed so it no longer assumes:

``` text
events.size() == 1
```

or that the Event created by the current test is the first element.
Instead, it captures the ID returned by `POST /events`, performs
`GET /events`, filters the returned collection by that ID, and verifies
the matching Event.

``` text
POST Event X
    ↓
capture X.id
    ↓
GET /events
    ↓
filter(event.id == X.id)
    ↓
findFirst().orElseThrow()
    ↓
verify Event X
```

This makes the assertion independent of unrelated Events already stored
in the singleton in-memory adapter without changing production scope
only for testing.

The current WebTestClient suite now covers successful create/list/find
flows, a valid but missing Event ID returning 404, and a malformed Event
ID returning 400. The full test suite is green.

------------------------------------------------------------------------

This is the current stopping point of the architecture documentation.
Request validation and reactive database persistence remain future
course steps.

------------------------------------------------------------------------

---

## Request Validation at the HTTP Boundary

Event creation now validates HTTP input before it reaches the
application use case.

`CreateEventRequest` uses Jakarta Bean Validation constraints for the
rules studied so far, including required text fields, maximum Event name
length, positive capacity, non-negative price and required start
date/currency.

The request flow is:

``` text
HTTP JSON
    ↓
Jackson deserialization
    ↓
CreateEventRequest
    ↓
@Valid / Bean Validation
    ├── valid   → CreateEventCommand → application
    └── invalid → WebExchangeBindException → 400 Bad Request
```

This reinforces the distinction between boundary validation and domain
invariants. HTTP validation rejects malformed or incomplete request data
early, while the domain still protects its own business rules
independently.

Validation errors are translated by the WebFlux exception handler into a
`ValidationErrorResponse` containing a timestamp, HTTP status, message
and field-error map.

------------------------------------------------------------------------

---

## Booking Domain and Aggregate

A second business module, `booking`, has now been introduced.

The Booking model studied so far contains:

``` text
Booking
│
├── BookingId
├── CustomerId
├── EventId
├── places
└── BookingStatus
```

A Booking is created through its factory method with a generated
`BookingId` and starts in `CONFIRMED` status. The number of places must
be greater than zero.

The lifecycle currently studied is intentionally small:

``` text
CONFIRMED
    │
    │ cancel()
    ▼
CANCELLED
```

Trying to cancel an already cancelled Booking is rejected with
`IllegalStateException`.

Booking domain tests cover successful creation, cancellation, repeated
cancellation rejection and invalid place counts. A JUnit 5 parameterized
test with `@ParameterizedTest` and `@ValueSource` was introduced to test
multiple invalid place values.

------------------------------------------------------------------------

---

## Cross-Module Coordination Through an Input Port

Creating a Booking requires reserving capacity in an Event. The Booking
application layer does not access `EventRepository` directly.

Instead, the Event module exposes an application input port:

``` text
ReserveEventPlacesUseCase
```

The dependency direction is:

``` text
Booking application
        ↓
ReserveEventPlacesUseCase       Event application API
        ↑
ReserveEventPlacesService
        ↓
EventRepository
        ↓
Event Aggregate
```

This preserves the module boundary: another module communicates with
Event through its application API rather than reaching into Event
persistence.

Inside the Event module, `ReserveEventPlacesService` loads the Event
through `EventRepository`, converts an empty result into
`EventNotFoundException`, tells the Aggregate to reserve the requested
places and saves the updated Event.

The domain remains responsible for the actual reservation rules: the
Event must be published and enough capacity must be available.

------------------------------------------------------------------------

---

## Reactive Booking Creation Flow

`CreateBookingService` coordinates the two operations studied so far:

``` text
CreateBookingCommand
        ↓
reserve Event places
        ↓
Mono<Event>
        ↓ flatMap
create/save Booking
        ↓
Mono<Booking>
```

`flatMap()` is required because reserving Event places returns a
Publisher and the next operation, saving the Booking, also returns a
Publisher.

An important Reactor rule was reinforced while implementing this flow:
operators create new Publishers. Calling a reactive method and ignoring
the returned `Mono` does not add that operation to the subscribed
pipeline.

The application tests use `StepVerifier` and in-memory fake
repositories. They currently verify successful Booking creation and
propagation of an insufficient-capacity error. The success scenario also
verifies the generated Booking data and the Event association.

Common test dependencies are recreated before every test using JUnit 5
`@BeforeEach`, keeping the stateful fake repositories isolated between
test methods.

------------------------------------------------------------------------

---

## Publish Event Application Use Case

Event publication is now exposed through an explicit application input
port:

``` text
PublishEventUseCase
        ↑
PublishEventService
        ↓
EventRepository
        ↓
Event.publish()
        ↓
EventRepository.update(...)
```

`PublishEventService` loads the Event, translates an empty repository
result into `EventNotFoundException`, invokes the synchronous domain
behaviour `event.publish()`, and composes the reactive save operation
with `flatMap()`.

The service is tested for both the successful `DRAFT → PUBLISHED`
transition and the missing-Event error path.

------------------------------------------------------------------------

---

## Booking HTTP Input Adapter

The Booking module now exposes:

``` text
POST /bookings
```

The adapter keeps the HTTP contract separate from the application and
domain models:

``` text
HTTP JSON
    ↓
CreateBookingRequest
    ↓
CreateBookingCommand
    ↓
CreateBookingUseCase
    ↓
Mono<Booking>
    ↓ map(...)
BookingResponse
    ↓
201 Created
```

`CreateBookingRequest` validates non-blank identifier strings and a
positive number of places. The web adapter converts identifier strings
into `UUID`, `CustomerId` and `EventId` values before creating the
application command.

Malformed UUID input is translated into the web-specific
`InvalidIdException` and mapped to `400 Bad Request`.

`BookingResponse` exposes the generated Booking ID, customer ID, Event
ID, number of places and Booking status.

------------------------------------------------------------------------

---

## Event Publication HTTP Endpoint

Event publication is now available through:

``` text
POST /events/{id}/publish
```

The Event identifier belongs in the URL because it identifies the
resource on which the publish operation is executed. No request body is
currently required because `publish()` needs no additional input.

The controller delegates to `PublishEventUseCase` rather than accessing
`EventRepository` directly.

------------------------------------------------------------------------

---

## Event Availability in the HTTP Contract

`EventResponse` now exposes both:

``` text
capacity   → total Event capacity
available  → currently available places
```

The complete Booking flow was manually verified with an Event whose
availability changed from `20` to `17` after creating a Booking for `3`
places.

------------------------------------------------------------------------

---

## Booking Controller Isolation Test

A focused `BookingControllerTest` has been added using Mockito and
`WebTestClient`.

`WebTestClient` is bound directly to a real `BookingController`, while
`CreateBookingUseCase` is replaced with a Mockito mock:

``` text
WebTestClient
      ↓
BookingController              real
      ↓
CreateBookingUseCase           mock
```

This isolates the HTTP adapter from the real application service and
repositories while verifying the `POST /bookings` HTTP contract and
`201 Created` response.

Spring configuration tests also verify that the Event and Booking
application dependencies are correctly wired.

------------------------------------------------------------------------

---

## Independent Booking Service — DDD and Hexagonal Architecture (October 2026)

> **Evolution through extraction and redesign.** Sections 37–54 document Booking **when it belonged to the original reactive monolith** (`CustomerId`, `places`, `CONFIRMED` on creation, Event-capacity orchestration, R2DBC and events). The new imperative `booking-service` is the **intended successor**, not a second permanent implementation. It intentionally uses **Spring MVC**, `UserId`, `PENDING` and **JPA/PostgreSQL persistence**. The former in-memory repository remains useful for learning and unit tests, but is no longer wired as the production repository. Historical and current domain contracts are different and must not be conflated. This architecture is selected to learn both paradigms, not claimed as an ideal production decomposition.

---

## Ports and adapters (current implementation)

```text
HTTP / OpenAPI DTOs
        ↓
BookingController implements generated BookingApi  [infrastructure, inbound]
        ↓
BookingUseCase (Spring proxy)                    [application input port]
        ↓
TransactionalBookingUseCase                     [infrastructure decorator]
        ↓
BookingService                                  [application, pure Java]
        ↓
BookingRepository                               [application output port]
        ↓
JpaBookingRepositoryAdapter                     [infrastructure, outbound]
        ↓
SpringDataBookingRepository / Hibernate / PostgreSQL
```

**Historical wiring (superseded by [synchronous compensation](PERSISTENCE_CONSISTENCY.md#synchronous-event-capacity-reservation-and-booking-compensation-16-october-2026)):** the following paragraph describes the initial transactional-only composition, before the capacity-compensation decorator was added.

The input port has `getBooking`, `createBooking`, `confirmBooking`, `cancelBooking`. The output port provides `findById`, `save` and `existsActiveBooking`. `BookingService` takes a `BookingRepository` in its constructor. `BookingConfiguration` registers one `BookingUseCase` bean: a `TransactionalBookingUseCase` wrapping the plain-Java service. The production repository is the JPA adapter; the previous in-memory bean registration was removed. The controller delegates via `BookingUseCase`; `BookingMapper` converts the returned domain entity into the generated response DTO. Dependency direction stays toward domain/application; Spring and HTTP are confined to adapters.

Actual package paths discussed for the new service include:

```text
com.rubenmarin.bookingservice
├── domain.model
├── application.exception
├── application.port.in
├── application.port.out
├── application.service
└── infrastructure
    ├── adapter.in (BookingController)
    ├── adapter.in.mapper (BookingMapper)
    ├── adapter.in.exception (GlobalExceptionHandler)
    ├── adapter.out.persistence (InMemoryBookingRepository, not wired in production)
    ├── adapter.out.persistence.jpa (BookingJpaEntity, SpringDataBookingRepository, JpaBookingRepositoryAdapter)
    ├── configuration (BookingConfiguration, TransactionalBookingUseCase)
    └── web.generated.api / web.generated.model
```

---

## API-first boundary and error translation

The new service's OpenAPI-generated `BookingApi`, `CreateBookingRequest`, `BookingResponse` and `ProblemDetail` are transport models, **not domain types**. The controller wraps incoming UUIDs in `EventId` and `UserId` and returns `ResponseEntity<BookingResponse>`. No `Mono`/`Flux` is needed in this **imperative** Booking Service.

- `POST /api/bookings` → `201 Created`; missing required `eventId` → `400 Bad Request`; active duplicate → `409 Conflict`.
- `GET /api/bookings/{id}` → `200 OK`; absent booking → `404 Not Found`.
- `GlobalExceptionHandler` returns `application/problem+json` using the **generated** `ProblemDetail` (distinct from Spring's class of the same name), with `type`, `title`, `status`, `detail`, `code` and `timestamp`.
- Validation errors use `MethodArgumentNotValidException`; a readable `detail` can be built from binding field errors. A malformed JSON body may require a separate `HttpMessageNotReadableException` handler; do not assume it is covered.

---

## Tested boundaries

- `@WebMvcTest(BookingController.class)` + `MockMvc` + `@MockitoBean` isolate the HTTP controller, request deserialization, mapper and advice from the real service/persistence. Tested response families: 200, 201, 400, 404 and 409.
- Mockito unit test of `BookingService` checks active duplicate rejection and `save()` never invoked.
- Real `InMemoryBookingRepository` tests run with pure JUnit and `@BeforeEach`, covering save/find, missing ID, replacing state and matching/nonmatching booking checks. Active and cancelled cases were subsequently reported green.

---

## Decision record — why imperative Spring MVC?

The new `booking-service` deliberately uses synchronous Spring MVC and JPA, to practice transactional persistence, concurrency and a classic Java stack alongside the **separate** EventHub WebFlux/R2DBC implementation. Reactive programming is not an intrinsic DDD or hexagonal requirement. It is a technology choice at the port/adapter boundary; the domain remains framework-independent in either approach.

**Still not implemented:** deterministic forcing of both concurrent existence checks to return false, Kafka, durable/idempotent distributed coordination and full deployment. A synchronous best-effort capacity reservation/compensation flow has since been implemented (see [synchronous compensation](PERSISTENCE_CONSISTENCY.md#synchronous-event-capacity-reservation-and-booking-compensation-16-october-2026)). Database-level uniqueness and a two-thread concurrent-start test have been implemented and verified.

---

## Booking Service → Event Service: REST output adapter (October 2026)

The new imperative Booking service defines `EventAvailabilityPort` in `application.port.out` with `boolean isAvailable(EventId eventId)`. Its infrastructure HTTP adapter, `RestEventAvailabilityAdapter`, implements the port using Spring `RestClient` and a deliberately small `EventResponse(UUID id, String status, int available)` projection. The Event service is still implemented with WebFlux/R2DBC, but Booking's call is synchronous; inter-service HTTP does not change either service's programming paradigm.

```text
BookingController (infrastructure, inbound)
  → BookingUseCase → BookingService (application)
    → BookingRepository (outbound port) → JPA adapter
    → EventAvailabilityPort (outbound port) → RestEventAvailabilityAdapter
        → RestClient → GET Event Service /events/{id}
```

Creation first checks `BookingRepository.existsActiveBooking(eventId, userId)` to reject duplicates without unnecessary remote calls. If there is no active duplicate it asks Event Service for availability, requiring `PUBLISHED` and `available > 0`, then persists `Booking` in `PENDING`. This is a **read-only availability check**, not a cross-service reservation; a race for the final capacity unit is still possible.

The adapter translates transport-specific failures to application-specific exceptions: HTTP 404 → `EventNotFoundException` (preserving cause); HTTP 5xx and `ResourceAccessException` → `EventServiceUnavailableException` (preserving cause). `GlobalExceptionHandler` maps these to RFC 9457 HTTP responses `404 EVENT_NOT_FOUND` and `503 EVENT_SERVICE_UNAVAILABLE`, while valid but unavailable events produce `409 EVENT_NOT_AVAILABLE`. Technical causes stay available to internal diagnostics, not public API error bodies.

Infrastructure `EventClientConfiguration` provides the `RestClient` with a configurable Event Service base URL, 2-second connection timeout and 3-second read timeout. `BookingConfiguration` wires the port into `BookingService`, retained behind `TransactionalBookingUseCase`; currently a blocking remote call occurs while the transactional decorator is active. That transaction does **not** extend into Event Service.

Tests: `RestEventAvailabilityAdapterTest` uses `MockRestServiceServer` for business-state responses, JSON with extra fields, remote 404, remote 500 and simulated I/O exceptions. `BookingServiceTest` tests rule decisions with mocked ports. `BookingHttpIT`, `TransactionalBookingUseCaseIT` and `ConcurrentBookingIT` use a Spring `@MockitoBean` for the external port while retaining real Spring/JPA/PostgreSQL integration where appropriate. Maven Failsafe executes `*IT` during `mvn verify` / `mvnw.cmd verify`. A real manual Event Service GET was validated. Actual timeout expiry, service-to-service tracing, and atomic distributed capacity updates have not been implemented/tested. Retry and Circuit Breaker are documented in §[Retry outbound adapter](#booking-service-retry-as-an-outbound-adapter-15-october-2026)–58.

---

## Booking Service: Retry as an outbound adapter (15 October 2026)

The application service depends on the `EventAvailabilityPort` abstraction. Infrastructure wraps `RestEventAvailabilityAdapter` with `RetryableEventAvailabilityAdapter`, so transient failure handling is implemented **outside** the domain and application layers. The infrastructure composition root (`EventClientConfiguration`) supplies the implementations; no Spring/Resilience4j annotations are required in the domain.

The Retry policy is implemented and unit-tested. **Configuration values, error classifications, decorator code and test scenarios are documented in [`RESILIENCE.md`](RESILIENCE.md#retry--resilience4j).**

---

## Booking Service: Circuit Breaker and resilience decorators (16 October 2026)

`BulkheadEventAvailabilityAdapter` now wraps `CircuitBreakerEventAvailabilityAdapter`, which wraps `RetryableEventAvailabilityAdapter` and then `RestEventAvailabilityAdapter`; all implement `EventAvailabilityPort`. This illustrates the **Decorator pattern**, dependency inversion and a hexagonal infrastructure boundary:

```text
BookingService (application)
  → EventAvailabilityPort (outbound port)
    → BulkheadEventAvailabilityAdapter
      → CircuitBreakerEventAvailabilityAdapter
        → RetryableEventAvailabilityAdapter
          → RestEventAvailabilityAdapter → Event Service
```

The **outer Bulkhead** limits concurrent logical operations to five, including their retries. The Circuit Breaker inside it evaluates the final outcome of Retry. OPEN rejects before further Retry/REST calls; infrastructure maps `CallNotPermittedException` and `BulkheadFullException` to the application exception `EventServiceUnavailableException`, keeping HTTP and Resilience4j out of the application layer. This protects availability, **not** atomic distributed Event-capacity reservation.

The decorators, their detailed failure policy, semaphore concurrency test, CLOSED/OPEN/HALF_OPEN transition tests and still-unverified behavior are explained in **[`RESILIENCE.md`](RESILIENCE.md#circuit-breaker--resilience4j)**. The matching work is tracked in [`ROADMAP.md`](ROADMAP.md#23-microservices-evolution).
