# Domain-Driven Design --- EventHub

This document contains the Domain-Driven Design concepts studied and
implemented so far in EventHub.

It is intentionally incremental: it documents only concepts that have
already been explained, implemented and tested in the project.

------------------------------------------------------------------------

# 1. Domain-First Design

EventHub started from the business domain rather than from a database or
REST API.

Instead of starting with:

``` text
Database
   ↓
Entity
   ↓
Repository
   ↓
Service
```

we started with:

``` text
Business
   ↓
Domain Model
   ↓
Business Rules
```

At this stage, the Event domain is implemented using pure Java.

It does not depend on:

-   Spring annotations
-   persistence
-   HTTP
-   WebFlux
-   Reactor
-   PostgreSQL

------------------------------------------------------------------------

# 2. Event Domain

The first model implemented is an Event.

An Event has:

``` text
Event
│
├── identity
├── name
├── description
├── start date
├── capacity
├── price
└── status
```

It also has business behaviour:

``` text
publish()
cancel()
reservePlaces()
releasePlaces()
```

This means the domain object contains both state and behaviour.

------------------------------------------------------------------------

# 3. Entity

An **Entity** is a domain object whose identity matters.

`Event` is an Entity.

For example, an Event may change from:

``` text
Event #A123
Status: DRAFT
```

to:

``` text
Event #A123
Status: PUBLISHED
```

It is still the same Event because its identity has not changed.

------------------------------------------------------------------------

# 4. EventId and Strongly Typed IDs

The Event identity is represented by:

``` java
EventId
```

instead of using `UUID` directly throughout the domain.

Implementation:

``` java
public record EventId(UUID value) {

    public EventId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Event id cannot be null"
            );
        }
    }

    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }
}
```

This gives the Event identifier its own domain type.

A strongly typed identifier is safer than using the same primitive or
general-purpose type for every identifier.

Conceptually:

``` text
Event
  │
  └── EventId
          │
          └── UUID
```

------------------------------------------------------------------------

# 5. Value Objects

A **Value Object** represents a domain concept defined by its value
rather than by an independent identity.

The Value Objects implemented so far are:

``` text
EventId
EventName
Capacity
Money
```

Java records are useful for these objects because they provide immutable
components and value-based equality.

------------------------------------------------------------------------

# 6. EventName

Instead of representing the Event name only as:

``` java
String name;
```

the domain introduces:

``` java
EventName
```

Implementation:

``` java
public record EventName(String value) {

    private static final int MAX_LENGTH = 100;

    public EventName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Event name cannot be blank"
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Event name cannot exceed "
                    + MAX_LENGTH
                    + " characters"
            );
        }
    }
}
```

The important idea is:

``` text
An invalid EventName cannot exist.
```

The domain object protects its own validity.

------------------------------------------------------------------------

# 7. Capacity

Capacity represents both the total number of places and the currently
available places.

``` text
Capacity
│
├── total
└── available
```

Implementation:

``` java
public record Capacity(
        int total,
        int available
) {

    public Capacity {
        if (total <= 0) {
            throw new IllegalArgumentException(
                    "Total capacity must be greater than zero"
            );
        }

        if (available < 0) {
            throw new IllegalArgumentException(
                    "Available capacity cannot be negative"
            );
        }

        if (available > total) {
            throw new IllegalArgumentException(
                    "Available capacity cannot exceed total capacity"
            );
        }
    }

    public static Capacity of(int total) {
        return new Capacity(total, total);
    }
}
```

------------------------------------------------------------------------

# 8. Domain Invariants

An invariant is a condition that must always remain true for the domain
object to remain valid.

Capacity currently protects these invariants:

``` text
total > 0

available >= 0

available <= total
```

Therefore invalid states such as these are rejected:

``` text
Capacity(0, 0)

Capacity(20, -1)

Capacity(20, 21)
```

The object cannot be constructed in those states.

------------------------------------------------------------------------

# 9. Factory Method

Capacity provides:

``` java
public static Capacity of(int total) {
    return new Capacity(total, total);
}
```

This allows:

``` java
Capacity.of(20);
```

instead of:

``` java
new Capacity(20, 20);
```

The factory method expresses that a newly created Capacity starts with
all places available.

------------------------------------------------------------------------

# 10. Behaviour in Value Objects

Capacity does not only contain data.

It also knows how places are reserved and released.

Reservation:

``` java
public Capacity reserve(int places) {

    if (places <= 0) {
        throw new IllegalArgumentException(
                "Places to reserve must be greater than zero"
        );
    }

    if (places > available) {
        throw new IllegalStateException(
                "Not enough available capacity"
        );
    }

    return new Capacity(
            total,
            available - places
    );
}
```

Release:

``` java
public Capacity release(int places) {

    if (places <= 0) {
        throw new IllegalArgumentException(
                "Places to release must be greater than zero"
        );
    }

    if (available + places > total) {
        throw new IllegalStateException(
                "Cannot release more places than total capacity"
        );
    }

    return new Capacity(
            total,
            available + places
    );
}
```

The behaviour is kept close to the data and invariants it affects.

------------------------------------------------------------------------

# 11. Tell, Don't Ask

The Capacity implementation demonstrates the idea:

``` text
Tell, Don't Ask
```

Instead of reading the state, calculating a new value externally and
rebuilding the object:

``` java
if (capacity.available() >= places) {
    capacity = new Capacity(
            capacity.total(),
            capacity.available() - places
    );
}
```

the caller tells Capacity what should happen:

``` java
capacity.reserve(places);
```

Capacity itself applies the corresponding rules.

------------------------------------------------------------------------

# 12. Immutability

`Capacity` is immutable.

For example:

``` java
Capacity original = Capacity.of(20);

Capacity updated = original.reserve(3);
```

Results in:

``` text
original
Capacity(20, 20)

updated
Capacity(20, 17)
```

The original Value Object remains unchanged.

The tests explicitly verify this behaviour.

------------------------------------------------------------------------

# 13. Money

Money is represented as a combination of:

``` text
amount
+
currency
```

instead of only:

``` java
BigDecimal price;
```

Implementation:

``` java
public record Money(
        BigDecimal amount,
        Currency currency
) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException(
                    "Amount cannot be null"
            );
        }

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative"
            );
        }

        if (currency == null) {
            throw new IllegalArgumentException(
                    "Currency cannot be null"
            );
        }
    }

    public static Money euros(BigDecimal amount) {
        return new Money(
                amount,
                Currency.getInstance("EUR")
        );
    }
}
```

Zero is allowed so EventHub can represent free Events.

------------------------------------------------------------------------

# 14. BigDecimal for Money

Money uses:

``` java
BigDecimal
```

rather than `double`.

Values are created using a decimal string:

``` java
new BigDecimal("49.99")
```

rather than:

``` java
new BigDecimal(49.99)
```

This avoids starting from a binary floating-point representation when an
exact decimal value is required.

------------------------------------------------------------------------

# 15. EventStatus

The Event lifecycle currently uses:

``` java
public enum EventStatus {

    DRAFT,
    PUBLISHED,
    CANCELLED
}
```

The current lifecycle is:

``` text
                  publish()
                     │
                     ▼
DRAFT ─────────────────────────► PUBLISHED
  │                                  │
  │ cancel()                         │ cancel()
  │                                  │
  ▼                                  ▼
CANCELLED                        CANCELLED
```

The status is not changed through an unrestricted setter.

Instead, Event exposes domain operations such as:

``` java
event.publish();
event.cancel();
```

------------------------------------------------------------------------

# 16. Aggregate Root

`Event` is the first Aggregate Root implemented in EventHub.

Current model:

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

External code performs Event-related business operations through
`Event`.

For example:

``` java
event.reservePlaces(3);
```

The Aggregate Root coordinates the rules required by that operation.

------------------------------------------------------------------------

# 17. Aggregate Behaviour

Reservation is implemented in Event as:

``` java
public void reservePlaces(int places) {

    if (status != EventStatus.PUBLISHED) {
        throw new IllegalStateException(
                "Places can only be reserved for published events"
        );
    }

    capacity = capacity.reserve(places);
}
```

This produces the following flow:

``` text
event.reservePlaces(3)
          │
          ▼
Event checks status
          │
          ▼
Capacity.reserve(3)
          │
          ▼
Capacity checks availability
```

Two domain rules are therefore protected:

``` text
Event must be PUBLISHED
        +
Enough capacity must be available
```

------------------------------------------------------------------------

# 18. Event Creation

The Event constructor is private.

Creation happens through:

``` java
Event.create(...)
```

The factory creates:

``` text
new Event
    │
    ├── generated EventId
    └── status = DRAFT
```

This prevents callers from choosing an arbitrary initial lifecycle state
when creating a new Event.

------------------------------------------------------------------------

# 19. Rich Domain Model

The Event domain is intentionally more than a collection of data fields.

Instead of:

``` java
event.setStatus(EventStatus.PUBLISHED);
```

we use:

``` java
event.publish();
```

Instead of calculating capacity outside the domain:

``` java
event.setAvailable(...);
```

we use:

``` java
event.reservePlaces(3);
```

The model therefore contains:

``` text
State
+
Behaviour
+
Business Rules
```

This is the rich domain model approach used so far in EventHub.

------------------------------------------------------------------------

# 20. Framework Independence

The domain currently contains no Spring annotations.

There is no:

``` java
@Entity
@Component
@Service
@Repository
@Document
```

inside the current domain model.

There are also no:

``` text
Mono
Flux
R2DBC
PostgreSQL
HTTP
Kafka
```

dependencies in it.

The current domain behaviour is pure synchronous Java logic.

------------------------------------------------------------------------

# 21. Domain Unit Tests

The domain is tested directly with JUnit.

No Spring context is required.

For example:

``` java
@Test
void shouldReservePlaces() {

    Capacity capacity = Capacity.of(20);

    Capacity updated = capacity.reserve(3);

    assertEquals(17, updated.available());
}
```

Current tests verify both successful behaviour and invalid states.

Capacity tests currently cover:

``` text
Create capacity with all places available
Reject zero total capacity
Reject negative available capacity
Reject available capacity greater than total
Reserve places
Preserve the original immutable Capacity
Reject reservation when capacity is insufficient
Release places
Reject release beyond total capacity
```

Event tests currently cover:

``` text
Create Event as DRAFT
Publish a DRAFT Event
Reject reservation for a DRAFT Event
Reserve places for a PUBLISHED Event
Reject reservation when capacity is insufficient
```

Money tests currently cover:

``` text
Create Money in EUR
Reject negative amount
Allow zero amount
```

The current domain test cycle is:

``` text
Change domain
      ↓
Run tests
      ↓
Green
```

using:

``` powershell
.\mvnw.cmd test
```

or:

``` powershell
.\mvnw.cmd clean test
```

------------------------------------------------------------------------

# 22. Current Domain Structure

``` text
com.rubenmarin.eventhub
│
└── event
    │
    └── domain
        │
        └── model
            ├── Event.java
            ├── EventId.java
            ├── EventName.java
            ├── EventStatus.java
            ├── Capacity.java
            └── Money.java
```

Current tests:

``` text
src/test/java/com/rubenmarin/eventhub
│
└── event
    └── domain
        └── model
            ├── CapacityTest.java
            ├── EventTest.java
            └── MoneyTest.java
```

This is the current stopping point of the DDD documentation.

The document will be extended only after additional DDD concepts have
been studied and implemented in the course.

------------------------------------------------------------------------

# 23. DDD and Hexagonal Architecture

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

# 24. Reactive Application Layer and Create Event Use Case

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

# 25. Reactive Outbound Port

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

# 26. Reactive Output Adapter and Lazy Execution

The in-memory output adapter was used first to study lazy reactive execution.
It remains available as a plain Java test adapter, while production Event persistence now uses the R2DBC adapter described later in this document.

`create()` and `update()` use `Mono.fromSupplier(...)` because the subscription lazily
produces a value while performing the in-memory save.

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

# 27. Query Input Port and Application Service

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

# 28. Dependency Inversion, Injection and Spring IoC

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

# 29. HTTP Input Adapter with Spring WebFlux

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

# 30. HTTP Contract Studied So Far

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

# 31. Reactive HTTP and Repository Flow

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

# 32. Tests at the Current Architecture Boundary

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

# 33. Current Architecture Structure

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

# 34. HTTP Error Handling

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

# 35. HTTP Integration Test Isolation

Earlier WebFlux integration tests exposed the lifecycle of the
in-memory repository when it was the Spring production adapter. The
production Event repository has since been replaced by the R2DBC adapter;
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

# 36. Request Validation at the HTTP Boundary

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

# 37. Booking Domain and Aggregate

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

# 38. Cross-Module Coordination Through an Input Port

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

# 39. Reactive Booking Creation Flow

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

# 40. Publish Event Application Use Case

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

# 41. Booking HTTP Input Adapter

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

# 42. Event Publication HTTP Endpoint

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

# 43. Event Availability in the HTTP Contract

`EventResponse` now exposes both:

``` text
capacity   → total Event capacity
available  → currently available places
```

The complete Booking flow was manually verified with an Event whose
availability changed from `20` to `17` after creating a Booking for `3`
places.

------------------------------------------------------------------------

# 44. Booking Controller Isolation Test

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

# 45. Reactive PostgreSQL Persistence with R2DBC

The Event module now has real reactive persistence backed by PostgreSQL 17 and Spring Data R2DBC.
PostgreSQL runs locally in Docker Compose and the application connects through an R2DBC URL.
The schema is initialized from `src/main/resources/schema.sql`.

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

The domain remains persistence-independent. `EventEntity` is an infrastructure persistence model annotated with `@Table("events")`; domain Value Objects are flattened into relational columns such as total/available capacity and amount/currency.

The adapter performs explicit mapping in both directions:

``` text
Event → EventEntity → PostgreSQL
PostgreSQL → EventEntity → Event.rehydrate(...) → Event
```

`Event.rehydrate(...)` reconstructs an existing Aggregate while preserving its persisted ID, status and available capacity. It is distinct from `Event.create(...)`, which represents creation of a new Aggregate.

# 46. Create vs Update Persistence Intent

Because `Event.create()` generates its UUID before persistence, a non-null `@Id` alone cannot tell Spring Data whether the row is new. This was observed in practice: Spring Data treated a new entity as an update, the HTTP request returned successfully, but no row was inserted.

`EventEntity` therefore implements `Persistable<UUID>` and exposes transient persistence metadata through `isNew`. The flag is not stored in PostgreSQL.

The application-owned repository port now makes persistence intent explicit:

``` text
create(Event) → new Event → isNew = true  → INSERT
update(Event) → existing Event → isNew = false → UPDATE
```

This keeps the persistence concern out of the domain. `CreateEventService` calls `create()`, while publication and capacity reservation call `update()`.

The in-memory adapter implements the same port, although both operations use `Map.put(...)` internally because a `HashMap` does not need to distinguish SQL INSERT from UPDATE.

The implementation was validated with the full green test suite and manually against PostgreSQL: creating Events produced persisted `DRAFT` rows and publishing an Event updated the same row to `PUBLISHED`.

Reactive transactions, database migrations and PostgreSQL Testcontainers have not yet been implemented.

------------------------------------------------------------------------

# 47. Booking PostgreSQL Persistence

The Booking module now also persists Bookings reactively in PostgreSQL through its own persistence adapter.

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

The Booking application layer still does not access `EventRepository` directly. Event owns its persistence, and Booking coordinates with Event through the Event application input port.

The complete flow was manually verified: creating a Booking for 3 places persisted the Booking and changed the corresponding Event availability from `20` to `17`.

At this point these are two successfully composed reactive persistence operations. Atomicity across both database writes has not yet been implemented or studied; reactive transactions remain a later step.


------------------------------------------------------------------------

# 48. Concurrent Booking and Optimistic Locking

A concurrency problem was reproduced around Event capacity. Two booking
operations can read the same persisted Event state before either update has
completed.

``` text
Event availability = 3

Booking A reads available = 3
Booking B reads available = 3

Booking A reserves 2
Booking B reserves 2
```

Each Aggregate instance can satisfy its domain invariant because each was
created from a snapshot that still contained 3 available places. Without
concurrency control, both operations could therefore proceed from stale state
and cause a lost update / business overselling problem.

The distinction studied is:

``` text
Domain invariant
    → protects one Aggregate instance

Concurrency control
    → protects persisted state when multiple operations race
```

The Event persistence model now uses optimistic locking with a version value.
The Event Aggregate preserves that version when existing state is rehydrated,
and the R2DBC persistence entity maps it using Spring Data's `@Version`.

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

This prevents a stale Event snapshot from silently overwriting the update that
won the race.

A focused concurrent-booking test reproduces the race and verifies the
optimistic-locking behaviour. The full test suite is green.

The operation that loses the optimistic-locking race is now handled by the
application. A persistence-specific optimistic-locking failure is translated at
the infrastructure boundary into `ConcurrentUpdateException`, an
application-level exception. This keeps Spring Data details out of the
application layer.

------------------------------------------------------------------------

# 49. Recovering the Booking That Loses the Race

An optimistic-locking conflict means that the Event state used by the losing
operation is stale. It does not necessarily mean that the Event is already full.

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

Re-reading is important because the domain rule must be evaluated against the
latest persisted capacity.

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

The retry is deliberately bounded to one additional attempt rather than being
an unlimited retry loop.

The concurrency integration test now verifies the resulting business state for
two concurrent Bookings of 2 places against an Event with capacity 3. After the
race settles, exactly one Booking is persisted and the Event has 1 available
place.

To verify persisted Bookings by Event, the Booking application now exposes a
query use case backed by `BookingRepository.findByEventId(...)`. The R2DBC
adapter delegates that query to a Spring Data derived query and maps the
resulting persistence entities back to domain `Booking` objects.

The test composes the concurrent attempt with the later verification using
Reactor. `then(...)` waits for the first Publisher to terminate successfully
while discarding its emitted value, then subscribes to the Publisher that reads
the final persisted state. `Flux.count()` converts the Booking query into a
`Mono<Long>` so the final Booking count can be asserted together with the
persisted Event.

Reactive transaction/atomicity across the Event update and Booking insert has
still not been implemented or studied.

------------------------------------------------------------------------

This is the current stopping point of the DDD and architecture documentation.
