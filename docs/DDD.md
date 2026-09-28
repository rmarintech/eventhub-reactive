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
