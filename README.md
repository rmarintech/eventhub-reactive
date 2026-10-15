# EventHub Reactive

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

> **Architecture update — 15 October 2026:** EventHub is deliberately **evolving from a reactive monolith to a mixed-paradigm microservices learning project**. The Booking capability is being **extracted and redesigned** as an independent **imperative Spring MVC service**, rather than retained as a second active Booking implementation. Historical WebFlux/R2DBC Booking material remains in the documentation as evidence of earlier work, **not as a parallel current service**. This is a conscious portfolio/learning decision to practise both paradigms; it is **not presented as an optimal production architecture**. The independent Booking service now has JPA/PostgreSQL persistence, Flyway migrations, Testcontainers integration tests and a transactional use-case decorator; distributed integration remains pending.

## Current components and ownership

| Component | Programming model | Persistence | Verified/documented capabilities |
| --- | --- | --- | --- |
| Event module (original EventHub application) | WebFlux / Reactor | PostgreSQL via R2DBC | Event API, event capacity, reactive transactions and optimistic locking |
| Booking module (historical, being replaced by extraction) | WebFlux / Reactor | PostgreSQL via R2DBC (historical) | `CustomerId` + places, `POST /bookings`, Event-capacity orchestration, reactive domain events |
| **`booking-service` (replacement under development)** | **Spring MVC / imperative** | **Spring Data JPA / PostgreSQL 17; Flyway** | `UserId`, PENDING/CONFIRMED/CANCELLED, API-first endpoints, MVC tests, JPA integration tests with Testcontainers, transactional decorator and verified rollback |

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

**Current limitation:** the `existsActiveBooking()` then `save()` check is not sufficient to guarantee uniqueness under concurrent requests, even when the application operation is transactional. A database-level partial unique index, concurrency tests and cross-service workflows are still pending.

### Booking persistence and transaction milestone — 15 October 2026

- Independent `booking-service/` application: Spring MVC / imperative HTTP adapter, Spring Data JPA / Hibernate, and PostgreSQL 17.
- Flyway migration `V1__create_bookings_table.sql` creates `bookings(booking_id, event_id, user_id, status)`; schema managed by migrations, not Hibernate auto-DDL.
- `BookingJpaEntity` maps persisted UUIDs and status to and from the immutable `Booking` domain model using `Booking.rehydrate(...)`. `JpaBookingRepositoryAdapter` implements the existing `BookingRepository` output port.
- JPA integration tests verified insert/read, active-booking lookup, exclusion of cancelled bookings, and update `PENDING → CONFIRMED` using `flush()`/`clear()`.
- PostgreSQL Testcontainers with `@ServiceConnection` runs integration tests on an isolated database; Flyway initializes its schema.
- The Spring-managed `TransactionalBookingUseCase` decorator implements `BookingUseCase` and declares `@Transactional` boundaries without adding Spring dependencies to `BookingService`. `BookingConfiguration` wraps the plain-Java service in this decorator.
- Integration tests **without test-level `@Transactional`** verified booking persistence and rollback after a deliberately failing delegate. A negative control removing the decorator's `@Transactional` caused the expected failure (the previously saved booking remained in PostgreSQL).
- **Still pending:** concurrency-safe uniqueness, a PostgreSQL partial unique index for active bookings, corresponding error translation, distributed Event-capacity coordination and Kafka integration.

### Documentation map

- [`docs/ROADMAP.md`](docs/ROADMAP.md): complete project progress; new independent service in section 26.
- [`docs/DDD.md`](docs/DDD.md): original domain and architecture, plus new service model/hexagonal boundaries in section 55.
- [`docs/REACTIVE.md`](docs/REACTIVE.md): original EventHub reactive learning and code only; ends with explicit separation of the new imperative service.


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
| Hexagonal Architecture | [DDD.md](docs/DDD.md#23-ddd-and-hexagonal-architecture) and [independent service design](docs/DDD.md#55-independent-booking-service--ddd-and-hexagonal-architecture-october-2026) |
| Reactive Programming | [REACTIVE.md](docs/REACTIVE.md) |

Additional documentation will be created when the corresponding topics
are reached in the course.

------------------------------------------------------------------------

# 🗺️ Current Position

``` text
Project initialization              ✅
        ↓
DDD fundamentals / Event Aggregate  ✅
        ↓
Hexagonal Architecture              🚧 IN PROGRESS
        ↓
Application / Ports / Adapters      ✅
        ↓
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux                      🚧 IN PROGRESS
        ↓
Reactive HTTP input adapter         ✅
        ↓
POST /events                        ✅
GET  /events                        ✅
GET  /events/{id}                   ✅
        ↓
HTTP error handling                 ✅
        ↓
Request validation                  ✅
        ↓
Booking domain / application flow   ✅
        ↓
Publish Event API                   ✅
        ↓
Booking HTTP API                    ✅
        ↓
Event R2DBC / PostgreSQL            ✅
        ↓
Booking R2DBC / PostgreSQL          ✅
        ↓
Concurrency / optimistic locking    ✅
        ↓
Reactive transaction / atomicity    ✅
        ↓
Domain Events                       ✅
        ↓
Domain Events vs Integration Events ✅
        ↓
Kafka fundamentals                  ⏸ DEFERRED (original reactive roadmap)
        ↓
Booking Service JPA / PostgreSQL    ✅
        ↓
Flyway / JPA integration tests      ✅
        ↓
Transactional decorator / rollback ✅
        ↓
Concurrent duplicate protection    🚧 NEXT
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

The independent Booking service additionally has JPA repository integration tests, PostgreSQL Testcontainers and tests of the transactional decorator (commit visibility, exception-triggered rollback and negative control without `@Transactional`). The Booking integration-test classes ending in `IT` should be run explicitly unless Maven Failsafe is configured. For example, from `booking-service/`:

```powershell
.\mvnw.cmd test "-Dtest=JpaBookingRepositoryAdapterIT,TransactionalBookingUseCaseIT,TransactionalBookingRollbackIT"
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
