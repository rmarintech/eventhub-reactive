# EventHub Reactive --- Project Roadmap

This roadmap tracks the complete learning and implementation path for
EventHub.

Completed items are marked as:

``` text
✅
```

The current or immediately upcoming area is marked as:

``` text
🚧 / NEXT
```

Future items remain unchecked until they are actually studied,
implemented and validated.

------------------------------------------------------------------------

# 1. Project Foundation

-   [x] Create EventHub project
-   [x] Java 21
-   [x] Spring Boot 4.1
-   [x] Maven
-   [x] Maven Wrapper
-   [x] Git repository
-   [x] Initial green test baseline
-   [x] Root README
-   [x] Documentation structure

------------------------------------------------------------------------

# 2. Domain-Driven Design Fundamentals

-   [x] Domain-first design
-   [x] Event domain
-   [x] Entity
-   [x] Entity identity
-   [x] Strongly Typed ID
-   [x] `EventId`
-   [x] Value Objects
-   [x] Java records for Value Objects
-   [x] `EventName`
-   [x] `Capacity`
-   [x] Domain invariants
-   [x] Factory methods
-   [x] Behaviour inside Value Objects
-   [x] Tell, Don't Ask
-   [x] Immutability
-   [x] `Money`
-   [x] `BigDecimal` for monetary values
-   [x] `EventStatus`
-   [x] Aggregate Root
-   [x] `Event` Aggregate Root
-   [x] Aggregate behaviour
-   [x] Controlled Event creation
-   [x] Rich Domain Model
-   [x] Framework-independent domain
-   [x] Pure domain unit tests
-   [x] `DDD.md`

------------------------------------------------------------------------

# 3. Hexagonal Architecture

-   [x] DDD vs Hexagonal Architecture
-   [x] Dependency direction
-   [x] Application layer
-   [x] Use cases
-   [x] Commands
-   [x] Inbound ports
-   [x] Application services
-   [x] Outbound ports
-   [x] Repository port
-   [x] Input adapters
-   [x] Output adapters
-   [x] Spring composition root / dependency wiring
-   [x] Architecture package structure
-   [ ] Architecture tests
-   [x] Hexagonal Architecture notes in DDD.md and Booking Service section
-   [ ] Dedicated HEXAGONAL.md (optional)

**Status:** IN PROGRESS --- HTTP input adapter is implemented; dedicated
architecture tests remain pending

------------------------------------------------------------------------

# 4. Domain Evolution

-   [ ] Domain-specific exceptions
-   [x] Domain events
-   [x] Booking domain-event tests
-   [x] Domain event publication port
-   [x] Application coordination of Domain Event publication
-   [x] In-memory Domain Event publisher adapter
-   [x] Persistence rehydration strategy
-   [ ] Additional Event use cases
-   [ ] Domain documentation update

------------------------------------------------------------------------

# 5. Reactive Programming Fundamentals

-   [x] Imperative vs reactive programming
-   [x] Blocking vs non-blocking
-   [x] Synchronous vs asynchronous
-   [x] Reactive Streams specification
-   [x] Publisher
-   [x] Subscriber
-   [x] Subscription
-   [x] Backpressure
-   [x] Project Reactor
-   [x] `Mono`
-   [x] `Flux`
-   [x] Subscription and lazy execution
-   [x] Cold publishers
-   [x] Hot publishers
-   [x] `map`
-   [x] `flatMap`
-   [x] `filter`
-   [x] `switchIfEmpty`
-   [x] `zip`
-   [x] `concatMap`
-   [x] `doOnNext`
-   [x] `doOnError`
-   [x] `onErrorResume`
-   [x] `retry`
-   [x] `timeout`
-   [x] Reactive error propagation
-   [x] Reactor schedulers
-   [x] `boundedElastic`
-   [x] `parallel`
-   [x] Thread model experiments
-   [x] Why `block()` breaks the reactive model
-   [x] Reactor testing with `StepVerifier`
-   [x] Reactive Programming documentation

------------------------------------------------------------------------

# 6. Spring WebFlux

-   [x] Add Spring WebFlux
-   [x] Servlet model vs reactive model
-   [ ] Netty
-   [x] Event Loop
-   [x] Reactive HTTP request lifecycle
-   [x] Reactive REST controllers
-   [x] `Mono` HTTP responses
-   [x] `Flux` HTTP responses
-   [x] Request validation
-   [x] Reactive exception handling
-   [x] Reactive DTO mapping
-   [x] `WebTestClient`
-   [x] WebFlux integration tests
-   [ ] WebFlux documentation

------------------------------------------------------------------------

# 7. Reactive Persistence

-   [x] PostgreSQL
-   [x] R2DBC
-   [x] JDBC vs R2DBC
-   [x] Configure reactive PostgreSQL connection
-   [x] Spring Data R2DBC
-   [x] Persistence model
-   [x] Domain ↔ persistence mapping
-   [x] Reactive repository adapter
-   [x] Reactive repository port
-   [x] Reactive CRUD
-   [x] Reactive transactions
-   [ ] Database migrations
-   [ ] Repository integration tests
-   [ ] PostgreSQL Testcontainers
-   [ ] R2DBC documentation

------------------------------------------------------------------------

# 8. Event API

-   [x] Create Event use case
-   [x] Find Event use case
-   [x] List Events use case
-   [x] Publish Event use case
-   [ ] Cancel Event use case
-   [x] Reactive Event REST API
-   [x] Request / response DTOs
-   [x] Validation
-   [x] Error responses
-   [x] Publish Event service tests
-   [x] Publish Event HTTP endpoint
-   [ ] API integration tests

------------------------------------------------------------------------

# 9. Booking Domain

> **Historical implementation:** completed items in sections 9–11 describe Booking **before its extraction**, inside the WebFlux/R2DBC monolith. The new Spring MVC `booking-service` is its **intended replacement**, not an additional permanent Booking implementation. See section 26.

-   [x] Booking domain analysis
-   [x] Booking Aggregate
-   [x] `BookingId`
-   [x] `CustomerId`
-   [x] Booking status
-   [x] Booking invariants
-   [x] Create Booking
-   [x] Cancel Booking
-   [x] Booking domain tests
-   [x] Booking application layer
-   [x] Booking ports
-   [x] Booking adapters
-   [x] Booking persistence
-   [x] Booking REST API
-   [x] Booking controller test with mocked use case

------------------------------------------------------------------------

# 10. Capacity & Concurrency

-   [x] Booking vs Event capacity coordination
-   [x] Concurrent booking scenario
-   [x] Overselling problem
-   [x] Reproduce race condition
-   [x] Evaluate concurrency-control strategy
-   [x] Implement optimistic locking with Event versioning
-   [x] Concurrent booking / optimistic-locking test
-   [x] Document optimistic-locking decision
-   [ ] Handle/recover the booking that loses the race
-   [x] Reactive transaction/atomicity across Event and Booking writes
-   [x] Verify rollback when Booking persistence fails after Event
    capacity update

------------------------------------------------------------------------

# 11. Event-Driven Architecture

-   [x] Domain Event fundamentals
-   [x] `BookingCreated` Domain Event
-   [x] Domain Event publisher output port
-   [x] Internal in-memory Domain Event publication
-   [x] Domain Events vs Integration Events
-   [x] Integration Event marker and publisher output port
-   [x] Booking Domain Event → Integration Event mapping
-   [x] Domain Event handler abstraction
-   [x] Domain Event dispatcher
-   [x] BookingCreated handler and dispatcher tests
-   [ ] Apache Kafka fundamentals
-   [ ] Kafka broker setup
-   [ ] Topics
-   [ ] Partitions
-   [ ] Offsets
-   [ ] Producers
-   [ ] Consumers
-   [ ] Consumer groups
-   [ ] Spring Kafka
-   [ ] Event contracts
-   [ ] Event serialization
-   [ ] Retry strategy
-   [ ] Dead Letter Topic
-   [ ] Idempotency
-   [ ] Duplicate-event handling
-   [ ] Eventual consistency
-   [ ] Transactional Outbox pattern
-   [ ] Kafka integration tests
-   [ ] Kafka documentation

------------------------------------------------------------------------

# 12. Security

-   [ ] Security fundamentals
-   [ ] Spring Security
-   [ ] Authentication
-   [ ] Authorization
-   [ ] Stateless security
-   [ ] OAuth2
-   [ ] OpenID Connect
-   [ ] JWT
-   [ ] Resource Server
-   [ ] Role-based authorization
-   [ ] Customer role
-   [ ] Organizer role
-   [ ] Protected Event operations
-   [ ] Protected Booking operations
-   [ ] Security tests
-   [ ] Security documentation

------------------------------------------------------------------------

# 13. React & TypeScript Fundamentals

-   [ ] TypeScript fundamentals
-   [ ] Create React application
-   [ ] Project structure
-   [ ] JSX / TSX
-   [ ] Components
-   [ ] Props
-   [ ] State
-   [ ] Events
-   [ ] Hooks
-   [ ] `useState`
-   [ ] `useEffect`
-   [ ] Forms
-   [ ] Conditional rendering
-   [ ] Lists and keys
-   [ ] React Router
-   [ ] Frontend testing fundamentals
-   [ ] React documentation

------------------------------------------------------------------------

# 14. Frontend ↔ Backend Integration

-   [ ] API client
-   [ ] Environment configuration
-   [ ] Event list
-   [ ] Event detail
-   [ ] Booking form
-   [ ] Booking list
-   [ ] Loading states
-   [ ] Error states
-   [ ] Authentication flow
-   [ ] Authorization-aware UI
-   [ ] Organizer Event management
-   [ ] End-to-end frontend/backend flow

------------------------------------------------------------------------

# 15. Real-Time Features

-   [ ] Reactive streaming concepts for frontend
-   [ ] Server-Sent Events
-   [ ] Live Event availability
-   [ ] Live booking updates
-   [ ] React real-time subscription
-   [ ] Reconnection/error handling
-   [ ] Real-time integration tests

------------------------------------------------------------------------

# 16. Testing

-   [ ] Complete domain unit-test suite
-   [x] Application tests with fake ports
-   [x] Mockito
-   [x] Reactor `StepVerifier`
-   [x] WebFlux `WebTestClient`
-   [ ] R2DBC integration tests
-   [ ] Testcontainers
-   [ ] Kafka integration tests
-   [ ] Security tests
-   [x] Full backend HTTP integration tests
-   [ ] Frontend tests
-   [ ] End-to-end tests
-   [ ] Architecture tests
-   [ ] Testing documentation

------------------------------------------------------------------------

# 17. Docker

-   [ ] Backend Dockerfile
-   [ ] Frontend Dockerfile
-   [ ] Multi-stage builds
-   [ ] Non-root containers
-   [x] PostgreSQL container
-   [ ] Kafka container
-   [x] Docker Compose
-   [ ] Container networking
-   [ ] Health checks
-   [ ] Secrets
-   [ ] Complete local environment
-   [ ] Docker documentation

------------------------------------------------------------------------

# 18. CI/CD

-   [ ] GitHub Actions
-   [ ] Backend build
-   [ ] Backend tests
-   [ ] Frontend build
-   [ ] Frontend tests
-   [ ] Architecture tests
-   [ ] Integration tests
-   [ ] Docker image build
-   [ ] GitHub Container Registry
-   [ ] Immutable commit-SHA image tags
-   [ ] Continuous Delivery workflow
-   [ ] CI/CD documentation

------------------------------------------------------------------------

# 19. Observability

-   [ ] Spring Boot Actuator
-   [ ] Micrometer
-   [ ] Application metrics
-   [ ] Business metrics
-   [ ] Prometheus
-   [ ] PromQL
-   [ ] Grafana
-   [ ] Dashboards
-   [ ] Structured logging
-   [ ] Distributed tracing
-   [ ] OpenTelemetry
-   [ ] Trace/log correlation
-   [ ] Reactive pipeline observability
-   [ ] SLIs / SLOs
-   [ ] Alerts
-   [ ] Observability documentation

------------------------------------------------------------------------

# 20. Performance & Reactive Behaviour

-   [ ] Reactive load testing
-   [ ] Throughput
-   [ ] Latency
-   [ ] Tail latency
-   [ ] Saturation
-   [x] Backpressure under load
-   [x] Event Loop blocking experiment
-   [ ] Blocking-call detection
-   [ ] Thread analysis
-   [ ] Connection-pool behaviour
-   [ ] Reactive bottleneck analysis
-   [ ] JVM profiling
-   [ ] Performance documentation

------------------------------------------------------------------------

# 21. Kubernetes

-   [ ] Kubernetes local environment
-   [ ] Namespaces
-   [ ] Backend Deployment
-   [ ] Frontend Deployment
-   [ ] Services
-   [ ] ConfigMaps
-   [ ] Secrets
-   [ ] PostgreSQL
-   [ ] Kafka connectivity
-   [ ] Startup probes
-   [ ] Readiness probes
-   [ ] Liveness probes
-   [ ] Persistent storage
-   [ ] Ingress
-   [ ] Resource requests / limits
-   [ ] Metrics Server
-   [ ] HPA
-   [ ] Horizontal scaling
-   [ ] Kubernetes documentation

------------------------------------------------------------------------

# 22. Helm

-   [ ] Helm chart
-   [ ] Templates
-   [ ] `values.yaml`
-   [ ] Environment configuration
-   [ ] Image tags
-   [ ] Helm lint
-   [ ] Install / upgrade
-   [ ] Rollback
-   [ ] Helm documentation

------------------------------------------------------------------------

# 23. Microservices Evolution

-   [x] Choose learning-oriented transition from reactive monolith to mixed-paradigm microservices
-   [x] Choose imperative Spring MVC for extracted Booking; retain reactive Event learning track
-   [ ] Review Modular Monolith boundaries
-   [ ] Review bounded contexts
-   [ ] Identify possible extraction boundary
-   [ ] Evaluate whether extraction is justified
-   [x] Begin extracting/rebuilding Booking from the reactive monolith as an imperative microservice
-   [ ] Verify full independent service deployment/extraction integration
-   [ ] Independent persistence ownership
-   [ ] Independent deployment
-   [ ] Reactive service-to-service communication
-   [ ] Event-driven service communication
-   [ ] Failure isolation
-   [ ] Timeouts
-   [ ] Retries
-   [ ] Circuit Breaker
-   [ ] Distributed consistency
-   [x] Document Booking Service status and architecture
-   [ ] Microservices documentation (complete distributed architecture)

------------------------------------------------------------------------

# 24. System Design

-   [ ] Final architecture review
-   [ ] Scalability
-   [ ] Availability
-   [ ] Data ownership
-   [ ] Consistency trade-offs
-   [ ] Caching
-   [ ] Messaging trade-offs
-   [ ] Failure scenarios
-   [ ] Capacity planning
-   [ ] Architecture diagrams
-   [ ] Architecture Decision Records
-   [ ] System Design documentation

------------------------------------------------------------------------

# 25. Portfolio & Interview Preparation

-   [ ] Final README review
-   [ ] Complete documentation review
-   [ ] Repository cleanup
-   [ ] Architecture diagrams
-   [ ] DDD interview questions
-   [ ] Hexagonal Architecture interview questions
-   [ ] Reactive Programming interview questions
-   [x] Project Reactor interview questions
-   [ ] Spring WebFlux interview questions
-   [ ] R2DBC interview questions
-   [ ] Kafka interview questions
-   [ ] Security interview questions
-   [ ] React interview questions
-   [ ] System Design interview questions
-   [ ] Final project walkthrough
-   [ ] CV / portfolio project description

------------------------------------------------------------------------

# Current Position

``` text
Project Foundation                  ✅
        ↓
DDD Fundamentals                    ✅
        ↓
Event Aggregate                     ✅
        ↓
Value Objects / Invariants          ✅
        ↓
Pure Domain Tests                   ✅
        ↓
DDD Documentation                   ✅
        ↓
Hexagonal Architecture              🚧 IN PROGRESS
        ↓
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux                      🚧 IN PROGRESS
        ↓
Reactive HTTP input adapter         ✅
        ↓
HTTP error handling                 ✅
        ↓
Request validation                  ✅
        ↓
Booking Domain / application flow   ✅
        ↓
Booking HTTP API                    ✅
        ↓
Event R2DBC / PostgreSQL            ✅
        ↓
Booking persistence                 ✅
        ↓
Concurrency / optimistic locking    ✅
        ↓
Domain Events                        ✅
        ↓
Domain vs Integration Events         ✅
        ↓
Kafka fundamentals                   ⏸ DEFERRED (original reactive track)
        ↓
Booking Service (Spring MVC / API)       ✅
        ↓
Booking Service JPA / PostgreSQL         ✅
        ↓
Flyway / PostgreSQL Testcontainers       ✅
        ↓
Transaction decorator / rollback tests  ✅
        ↓
Active-booking DB uniqueness            ✅
        ↓
Concurrency / HTTP integration tests     ✅
        ↓
Event ↔ Booking service integration       🚧 NEXT
        ↓
Cross-service integration / Kafka        ⬜ PLANNED
```

---

# 26. Independent Booking Service — Spring MVC (new track, October 2026)

> **Evolution, not parallel operation:** Sections 9–11 describe the **historical Booking module within the reactive monolith** (WebFlux, `CustomerId`, places, R2DBC/PostgreSQL and Event capacity). This section tracks its **extraction/redesign into the imperative `booking-service`**. Historical accomplishments remain documented, but do not count as features of the new service. The intentional mixed-stack approach serves learning and portfolio goals, not a claim of optimal production design.

## Implemented / practiced

- [x] Separate Booking domain model with `Booking`, `BookingId`, `EventId`, `UserId` and `BookingStatus`
- [x] Initial `PENDING` status; `confirm()` and `cancel()` domain operations
- [x] Immutable-style transition tested (new Booking instance retaining identifier)
- [x] Application input port `BookingUseCase`
- [x] Application service `BookingService`: get, create, confirm, cancel
- [x] Repository output port `BookingRepository`: `findById`, `save`, `existsActiveBooking`
- [x] In-memory output adapter (`InMemoryBookingRepository`)
- [x] Active booking query: matching event and user, status other than `CANCELLED`
- [x] Application rule: refuse creation when an active reservation already exists
- [x] `BookingNotFoundException` and `BookingAlreadyExistsException` in application layer
- [x] Spring MVC controller implementing generated `BookingApi`
- [x] OpenAPI-first contract (`api.yaml`), generated request/response/error models
- [x] `BookingMapper` converting domain Booking into generated `BookingResponse`
- [x] HTTP `GET /api/bookings/{id}` (200 or 404)
- [x] HTTP `POST /api/bookings` (201, 400 validation, or 409 conflict)
- [x] HTTP error representation: generated RFC 9457 `ProblemDetail`, `application/problem+json`
- [x] `GlobalExceptionHandler` for not-found, invalid request and duplicate booking
- [x] MVC-slice controller tests with `@WebMvcTest`, `MockMvc`, `@MockitoBean`
- [x] Controller cases 200, 201, 400, 404, 409 practiced
- [x] Unit test for duplicate-booking rejection using JUnit 5 and Mockito
- [x] In-memory repository tests for save, find, replacement and negative existence queries
- [x] Active status query implementation and positive/negative test scenarios discussed

## Next / not yet implemented in the new service

- [x] Run pending/confirmed/cancelled repository tests (reported green)
- [x] Run `BookingService` success and transition tests (reported green)
- [x] Verify Spring wiring and persistence through the `BookingUseCase` in a full integration-test context
- [ ] Define and test POST/GET error cases beyond the existing MVC scenarios
- [x] Spring Data JPA and PostgreSQL persistence adapter (`JpaBookingRepositoryAdapter`)
- [x] Domain-to-JPA mapping and `Booking.rehydrate(...)` round-trip test
- [x] DB schema management with Flyway `V1__create_bookings_table.sql` and `V2__unique_active_booking.sql`
- [x] Database-level duplicate protection and concurrent-request test (two threads, start synchronized; not forced simultaneous pre-check)
- [x] Implement and verify PostgreSQL partial unique index (Flyway V2) allowing rebooking after `CANCELLED`
- [x] PostgreSQL 17 Testcontainers database integration tests (`@ServiceConnection`)
- [x] JPA integration tests: insert, fetch, active-status checks and state update
- [x] Infrastructure transaction decorator (`TransactionalBookingUseCase`)
- [x] Commit-visibility test with no test-level `@Transactional`
- [x] Rollback test with a failing use-case delegate and negative control removing `@Transactional`
- [x] JPA adapter `saveAndFlush()` and selective constraint-name translation to `BookingAlreadyExistsException`
- [x] Unit test preserving unrelated `DataIntegrityViolationException` (Mockito)
- [x] PostgreSQL integration tests: reject duplicate active booking; permit `CANCELLED` + `PENDING`
- [x] `ConcurrentBookingIT`: exactly one successful and one rejected concurrent-start request
- [x] `TransactionalBookingUseCaseIT`: committed create, confirm, cancel from pending and cancel after confirmation
- [x] `BookingHttpIT`: real Spring MVC/MockMvc + PostgreSQL HTTP 201 and duplicate 409
- [ ] Optional deterministic concurrency test forcing both pre-checks to report no booking
- [ ] Decide how the extracted Booking service coordinates Event availability/capacity (previous monolith logic does not transfer automatically)
- [ ] Service-to-service Event availability and capacity coordination
- [ ] Cross-service consistency, failures and compensation strategy
- [ ] Kafka-based integration events / reliable outbox (if selected)
- [ ] Containerization, deployment and CI/CD for the independent service

**Concurrency caveat:** the `existsActiveBooking()` then `save()` sequence can still race even inside `@Transactional`, but PostgreSQL now prevents two active duplicates through Flyway V2's partial unique index. `@Version` alone would not prevent inserts with distinct Booking IDs. The two-thread test did not deterministically force both pre-checks to return false.

## Next milestone

Design and implement communication between the reactive Event side and imperative Booking service, beginning with clear ownership of Event availability/capacity and defining service-to-service contracts. Distributed consistency, failure recovery, Kafka and outbox remain pending.
