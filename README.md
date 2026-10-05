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

Planned in the project roadmap: - Modular Monolith - Event-Driven
Architecture

## Frontend

Planned:

-   TypeScript
-   React

## Persistence

Planned:

-   PostgreSQL
-   R2DBC

## Infrastructure

Planned:

-   Docker
-   Docker Compose
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

pic Do                     cumentation
  -------------------------- --------------------------------------------------------
oject progress \[R         OADMAP.md\](docs/ROADMAP.md)
main-Driven Design \[D     DD.md\](docs/DDD.md)
xagonal Architecture \[D   DD.md\](docs/DDD.md#23-ddd-and-hexagonal-architecture)
active Programming \[R     EACTIVE.md\](docs/REACTIVE.md)

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
Publish Event API                    ✅
        ↓
Booking HTTP API                    ✅
        ↓
R2DBC / PostgreSQL                  🚧 NEXT
```

For the complete project plan, check [ROADMAP.md](docs/ROADMAP.md).

------------------------------------------------------------------------

# 🏗️ Technical Picture

The project now contains framework-independent Event and Booking domain
models, reactive application ports, in-memory reactive output adapters,
Spring WebFlux HTTP input adapters for Event and Booking, and Spring IoC
configuration for dependency wiring. Booking creation coordinates Event
capacity through an Event application input port rather than accessing
Event persistence directly. Event publication is also exposed as an
explicit application use case.

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
InMemoryEventRepository                      OUTPUT ADAPTER
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

The domain remains independent of Spring and Reactor. Spring-specific
HTTP, persistence adapters and dependency wiring live in infrastructure.

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

The current build can be verified with:

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
`Reactive HTTP`
