# EventHub Reactive

A full-stack event management and booking platform built with **Java 21
and Spring Boot**, with React planned for the frontend.

The project is designed as a practical **Senior Java portfolio project**, with a strong focus on reactive Java development,
Domain-Driven Design (DDD), Hexagonal Architecture and modern full-stack
development.

The application is being developed incrementally. Technologies and
architectural patterns are introduced when they are
needed, and the documentation is updated only after they have been
studied and implemented.

------------------------------------------------------------------------

# 🚀 Tech Stack

## Backend

-   Java 21
-   Spring Boot 4.1
-   Maven
-   JUnit 5
-   Project Reactor
-   Reactor Test / StepVerifier

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
-   Output adapter
-   Reactive Programming fundamentals
-   Reactive Streams concepts
-   Mono / Flux
-   Reactive error handling
-   Cold and hot publishers
-   Reactor schedulers and threading fundamentals
-   Blocking vs non-blocking execution

Planned in the project roadmap:
-   Modular Monolith
-   Event-Driven Architecture

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

Topic                     Documentation
  ------------------------- -------------------------------
Project progress          [ROADMAP.md](docs/ROADMAP.md)
Domain-Driven Design      [DDD.md](docs/DDD.md)
Hexagonal Architecture    [DDD.md](docs/DDD.md#23-ddd-and-hexagonal-architecture)
Reactive Programming      [REACTIVE.md](docs/REACTIVE.md)

Additional documentation will be created when the corresponding topics
are reached in the course.

------------------------------------------------------------------------

# 🗺️ Current Position

``` text
Project initialization             ✅
        ↓
Java 21                            ✅
        ↓
Spring Boot 4.1                    ✅
        ↓
Maven / Maven Wrapper              ✅
        ↓
DDD fundamentals                   ✅
        ↓
Event domain                       ✅
        ↓
Entity / Value Objects             ✅
        ↓
Strongly Typed EventId             ✅
        ↓
Domain invariants                  ✅
        ↓
Rich Domain Model                  ✅
        ↓
Event Aggregate Root               ✅
        ↓
Pure domain unit tests             ✅
        ↓
DDD documentation                  ✅
        ↓
Hexagonal Architecture             🚧 IN PROGRESS
        ↓
Application / Ports / Output Adapter ✅
        ↓
Spring IoC wiring                    ✅
        ↓
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux                      🚧 NEXT
```

For the complete project plan, check [ROADMAP.md](docs/ROADMAP.md).

------------------------------------------------------------------------

# 🏗️ Technical Picture

The project now contains a framework-independent domain, an application layer with explicit ports, an in-memory output adapter and Spring IoC configuration for dependency wiring.

``` text
External input adapter                       ⏳ WebFlux later
        │
        ▼
CreateEventUseCase                           PORT IN
        ▲
        │ implements
CreateEventService                           APPLICATION
        │
        ├────────────► Event                 DOMAIN
        │
        ▼
EventRepository                              PORT OUT
        ▲
        │ implements
InMemoryEventRepository                      OUTPUT ADAPTER

Spring EventConfiguration                    IoC / WIRING
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

The domain and application service remain independent of Spring. Spring-specific wiring and the current output adapter live in infrastructure.

------------------------------------------------------------------------

# 🏛️ Current Domain Model

The first domain model implemented in EventHub is the Event aggregate.

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

The project now contains pure domain unit tests, an application-service test using a fake repository port, an output-adapter test, a Spring ApplicationContext wiring test and focused Reactor learning tests.

Domain and application unit tests require no Spring ApplicationContext, database, Docker or HTTP server. The dedicated configuration test intentionally starts a Spring ApplicationContext to validate IoC wiring.

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
`Project Reactor` · `Mono` · `Flux` · `StepVerifier` · `Reactor Schedulers`
