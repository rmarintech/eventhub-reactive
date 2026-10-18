# Domain-Driven Design — EventHub


**Current event-integration boundary (18 October 2026):** the independent Booking Service now records `BookingCreatedIntegrationEvent` as the JSON payload of a PostgreSQL Outbox record in the same transaction as Booking creation. The integration event is an external message contract, not the `Booking` aggregate or the persisted Outbox record. Kafka publication details: [KAFKA.md](KAFKA.md). Historical statements below about Kafka not yet being implemented refer to earlier milestones.

Business modeling, aggregates, value objects, invariants and domain events in the original reactive EventHub and the independent Booking Service. Architecture and persistence are documented separately in [HEXAGONAL.md](HEXAGONAL.md) and [PERSISTENCE_CONSISTENCY.md](PERSISTENCE_CONSISTENCY.md).

---

## Domain-First Design

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

---

## Event Domain

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

---

## Entity

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

---

## EventId and Strongly Typed IDs

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

---

## Value Objects

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

---

## EventName

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

---

## Capacity

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

---

## Domain Invariants

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

---

## Factory Method

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

---

## Behaviour in Value Objects

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
        throw new InsufficientCapacityException(
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
        throw new ExcessiveCapacityException(
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

---

## Tell, Don't Ask

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

---

## Immutability

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

---

## Money

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

---

## BigDecimal for Money

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

---

## EventStatus

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

---

## Aggregate Root

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

---

## Aggregate Behaviour

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

---

## Event Creation

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

---

## Rich Domain Model

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

---

## Framework Independence

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

---

## Domain Unit Tests

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

---

## Current Domain Structure

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

## Booking Domain Events

A Domain Event represents a business fact that has already happened.
EventHub now uses the framework-independent `DomainEvent` marker
interface and the `BookingCreated` record.

``` text
CreateBooking    → command: asks for an action
BookingCreated   → event: states a fact that already happened
```

`Booking.generate(...)` owns creation of `BookingCreated` and registers
it using the same generated `BookingId`. The Aggregate exposes an
unmodifiable copy of its current domain events. These events are
transient facts produced during the current Aggregate execution;
`Booking.rehydrate(...)` therefore starts with an empty domain-event
collection.

Domain tests verify that generating a Booking registers one
`BookingCreated` with the expected Booking, customer, Event and places
data, while rehydrating an existing Booking does not register a new
event.

---

## Domain Event Publication Port

Registering and publishing are separate responsibilities:

``` text
Domain         → registers the business fact
Application    → coordinates publication
Infrastructure → provides the publication mechanism
```

The application owns the output port `DomainEventPublisher`, whose
`publishDomainEvent(DomainEvent)` operation returns `Mono<Void>`.

`CreateBookingService` now composes publication after Booking
persistence:

``` text
reserve Event places
        ↓
persist Booking
        ↓
read Booking domain events
        ↓
publish BookingCreated
        ↓
publication completes
        ↓
return Booking
```

Infrastructure provides `InMemoryDomainEventPublisher`, which now
delegates publication to `DomainEventDispatcher`. The dispatcher
receives the available `DomainEventHandler<?>` beans and routes each
Domain Event to the handler whose `eventType()` matches the concrete
event class.

The application test configures a Mockito publisher returning
`Mono.empty()` and verifies that `publishDomainEvent(...)` is called
with `BookingCreated`. The complete test suite is green.

Kafka publication has not yet been implemented.

------------------------------------------------------------------------

---

## Domain Events vs Integration Events

The distinction between Domain Events and Integration Events has now
been studied and implemented.

``` text
Domain Event
    → internal business fact
    → shaped by the domain

Integration Event
    → external communication contract
    → shaped for communication outside the domain/module
```

`BookingCreated` remains a Domain Event. The application maps it to the
separate `BookingCreatedIntegrationEvent` contract instead of exposing
the Domain Event directly to external consumers.

The shared application layer defines the framework-independent
`IntegrationEvent` marker and the `IntegrationEventPublisher` output
port. `BookingIntegrationEventMapper` owns the transformation from the
internal Booking fact to the outward integration contract.

---

## Domain Event Handler and Dispatcher

Application reactions to Domain Events are represented by
`DomainEventHandler<T extends DomainEvent>`. A handler exposes both
`handle(T event)` and `eventType()`.

`BookingCreatedHandler` handles `BookingCreated`, maps it to
`BookingCreatedIntegrationEvent`, and delegates publication through
`IntegrationEventPublisher`.

``` text
BookingCreated
    ↓
DomainEventPublisher
    ↓
InMemoryDomainEventPublisher
    ↓
DomainEventDispatcher
    ↓
BookingCreatedHandler
    ↓
BookingIntegrationEventMapper
    ↓
BookingCreatedIntegrationEvent
    ↓
IntegrationEventPublisher
```

Spring wiring keeps module ownership explicit: shared configuration
creates the dispatcher and shared publisher adapters, while Booking
configuration registers the Booking mapper and `BookingCreatedHandler`.

A focused dispatcher test verifies that a `BookingCreated` event is
routed to the matching `DomainEventHandler<BookingCreated>`. The
complete test suite is green.

Kafka is still the next transport to be studied; no Kafka implementation
is documented yet.


---

---

## New Booking aggregate and identifiers

The independent service's business concepts are:

```text
Booking (domain entity)
├── BookingId (strongly typed UUID identifier)
├── EventId (strongly typed UUID identifier)
├── UserId (strongly typed UUID identifier)
└── BookingStatus = PENDING | CONFIRMED | CANCELLED
```

`new Booking(eventId, userId)` generates a Booking ID and creates a `PENDING` booking. `booking.confirm()` and `booking.cancel()` express lifecycle transitions through domain behavior instead of an arbitrary status setter. The repository tests verify that `confirm()` returns a different instance retaining the same ID and leaves the original booking in `PENDING`. The exact set of invalid transitions must remain grounded in the domain code/tests when expanded.

`BookingId`, `EventId` and `UserId` are value-object wrappers over UUIDs; equality by value makes the controller's newly constructed identifiers match Mockito test arguments when implemented as Java records.

---

## Active booking uniqueness is a business rule

An *active* booking has the same `EventId` and `UserId` and a status other than `CANCELLED`. As currently implemented, both `PENDING` and `CONFIRMED` are active. Cancelling one permits a new booking for that pair at the application-rule level.

```java
public Booking createBooking(EventId eventId, UserId userId) {
    if (repository.existsActiveBooking(eventId, userId)) {
        throw new BookingAlreadyExistsException(eventId, userId);
    }
    return repository.save(new Booking(eventId, userId));
}
```

`BookingAlreadyExistsException` is in `application.exception`: it reports a rejected use case without depending on Spring HTTP. `BookingNotFoundException` is also an application exception. Both are mapped by infrastructure, not embedded in the domain.

**Concurrency safeguard:** This read-before-save check remains vulnerable to a check-then-act race. PostgreSQL now provides an atomic guarantee through a partial unique index for active statuses, with selective persistence-error translation. Optimistic locking on an existing versioned row addresses a different concurrent-update problem, not arbitrary duplicate inserts. See [Persistence and Consistency](PERSISTENCE_CONSISTENCY.md) for the database safeguard and concurrent-start tests.
