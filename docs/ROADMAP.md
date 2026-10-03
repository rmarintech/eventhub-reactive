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
-   [ ] Hexagonal Architecture documentation

**Status:** IN PROGRESS — HTTP input adapter is implemented; dedicated architecture tests remain pending

------------------------------------------------------------------------

# 4. Domain Evolution

-   [ ] Domain-specific exceptions
-   [ ] Domain events
-   [ ] Event domain-event tests
-   [ ] Persistence rehydration strategy
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
-   [ ] Request validation
-   [x] Reactive exception handling
-   [x] Reactive DTO mapping
-   [x] `WebTestClient`
-   [x] WebFlux integration tests
-   [ ] WebFlux documentation

------------------------------------------------------------------------

# 7. Reactive Persistence

-   [ ] PostgreSQL
-   [ ] R2DBC
-   [ ] JDBC vs R2DBC
-   [ ] Configure reactive PostgreSQL connection
-   [ ] Spring Data R2DBC
-   [ ] Persistence model
-   [ ] Domain ↔ persistence mapping
-   [ ] Reactive repository adapter
-   [ ] Reactive repository port
-   [ ] Reactive CRUD
-   [ ] Reactive transactions
-   [ ] Database migrations
-   [ ] Repository integration tests
-   [ ] PostgreSQL Testcontainers
-   [ ] R2DBC documentation

------------------------------------------------------------------------

# 8. Event API

-   [x] Create Event use case
-   [x] Find Event use case
-   [x] List Events use case
-   [ ] Publish Event use case
-   [ ] Cancel Event use case
-   [x] Reactive Event REST API
-   [x] Request / response DTOs
-   [ ] Validation
-   [x] Error responses
-   [ ] API integration tests

------------------------------------------------------------------------

# 9. Booking Domain

-   [ ] Booking domain analysis
-   [ ] Booking Aggregate
-   [ ] `BookingId`
-   [ ] `CustomerId`
-   [ ] Booking status
-   [ ] Booking invariants
-   [ ] Create Booking
-   [ ] Cancel Booking
-   [ ] Booking domain tests
-   [ ] Booking application layer
-   [ ] Booking ports
-   [ ] Booking adapters
-   [ ] Booking persistence
-   [ ] Booking REST API

------------------------------------------------------------------------

# 10. Capacity & Concurrency

-   [ ] Booking vs Event capacity coordination
-   [ ] Concurrent booking scenario
-   [ ] Overselling problem
-   [ ] Reproduce race condition
-   [ ] Evaluate concurrency-control strategy
-   [ ] Implement selected strategy
-   [ ] Concurrency integration tests
-   [ ] Document concurrency decisions

------------------------------------------------------------------------

# 11. Event-Driven Architecture

-   [ ] Domain Events vs Integration Events
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
-   [ ] Mockito
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
-   [ ] PostgreSQL container
-   [ ] Kafka container
-   [ ] Docker Compose
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

-   [ ] Review Modular Monolith boundaries
-   [ ] Review bounded contexts
-   [ ] Identify possible extraction boundary
-   [ ] Evaluate whether extraction is justified
-   [ ] Extract selected service if appropriate
-   [ ] Independent persistence ownership
-   [ ] Independent deployment
-   [ ] Reactive service-to-service communication
-   [ ] Event-driven service communication
-   [ ] Failure isolation
-   [ ] Timeouts
-   [ ] Retries
-   [ ] Circuit Breaker
-   [ ] Distributed consistency
-   [ ] Microservices documentation

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
Request validation                  🚧 NEXT
        ↓
R2DBC / PostgreSQL                  ⏳
        ↓
Booking Domain                      ⏳
        ↓
Concurrency                         ⏳
        ↓
Kafka / Event-Driven                ⏳
        ↓
Security                            ⏳
        ↓
React / TypeScript                  ⏳
        ↓
Full-Stack Integration              ⏳
        ↓
Real-Time Features                  ⏳
        ↓
Docker                              ⏳
        ↓
CI/CD                               ⏳
        ↓
Observability                       ⏳
        ↓
Performance                         ⏳
        ↓
Kubernetes / Helm                   ⏳
        ↓
Microservices Evolution             ⏳
        ↓
System Design                       ⏳
        ↓
Portfolio / Interview Preparation   ⏳
```
