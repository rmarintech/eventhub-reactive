# EventHub Reactive

A full-stack event management and booking platform built with **Java 21
and Spring Boot**, with React planned for the frontend.

The project is designed as a practical **Senior Java portfolio and
learning project**, with a strong focus on reactive Java development,
Domain-Driven Design (DDD), Hexagonal Architecture and modern full-stack
development.

The application is being developed incrementally. Technologies and
architectural patterns are introduced during the course when they are
needed, and the documentation is updated only after they have been
studied and implemented.

------------------------------------------------------------------------

# 🚀 Tech Stack

## Backend

-   Java 21
-   Spring Boot 4.1
-   Maven
-   JUnit 5

## Architecture

Currently introduced:

-   Domain-Driven Design (DDD)
-   Rich Domain Model
-   Entity
-   Value Objects
-   Aggregate Root
-   Domain invariants

Planned in the project roadmap:

-   Hexagonal Architecture
-   Ports and Adapters
-   Modular Monolith
-   Reactive Architecture
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
Course/project progress   [ROADMAP.md](docs/ROADMAP.md)
Domain-Driven Design      [DDD.md](docs/DDD.md)

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
Hexagonal Architecture             ⏳ NEXT
```

For the complete project plan, check [ROADMAP.md](docs/ROADMAP.md).

------------------------------------------------------------------------

# 🏗️ Technical Picture

The project currently contains a Spring Boot application with a
framework-independent Event domain.

``` text
eventhub-reactive
│
├── pom.xml
│
├── src/
│   ├── main/
│   │   └── java/com/rubenmarin/eventhub/
│   │       └── event/
│   │           └── domain/
│   │               └── model/
│   │                   ├── Event.java
│   │                   ├── EventId.java
│   │                   ├── EventName.java
│   │                   ├── EventStatus.java
│   │                   ├── Capacity.java
│   │                   └── Money.java
│   │
│   └── test/
│       └── java/com/rubenmarin/eventhub/
│           └── event/
│               └── domain/
│                   └── model/
│                       ├── CapacityTest.java
│                       └── EventTest.java
│
└── docs/
    ├── DDD.md
    └── ROADMAP.md
```

The domain currently has no dependency on Spring infrastructure,
persistence or reactive frameworks.

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

The current tests are pure domain unit tests.

They require:

``` text
NO Spring ApplicationContext
NO database
NO Docker
NO HTTP server
```

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
`Rich Domain Model`
