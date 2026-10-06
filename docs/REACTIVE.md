# Reactive Programming --- EventHub

This document contains only the Reactive Programming concepts already
studied and tested in EventHub.

## 1. Core model

We studied **imperative vs reactive**, **blocking vs non-blocking**, and
**synchronous vs asynchronous**. Reactive programming models work as
streams of signals; it does not automatically mean asynchronous or
multithreaded execution.

## 2. Reactive Streams

The model studied contains `Publisher`, `Subscriber`, `Subscription` and
`Processor`. Backpressure lets a subscriber express how much data it is
ready to receive. Project Reactor implements this model for Java.

## 3. Mono and Flux

``` text
Mono<T> → 0..1 values
Flux<T> → 0..N values
```

Signals studied are `onNext`, `onComplete` and `onError`. `onError` and
`onComplete` are terminal signals.

## 4. Subscription and lazy execution

Building a pipeline does not necessarily execute it. Subscription
triggers execution.

``` text
build pipeline → recipe exists → subscribe → execution starts
```

Studied creation methods:

``` text
Mono.just(value)       → value already available
Mono.fromSupplier(...) → lazy value
Mono.fromCallable(...) → lazy Callable-based operation
Mono.defer(...)        → lazy Publisher creation
```

## 5. Operators

`map()` transforms values, `filter()` filters them, `flatMap()` flattens
nested publishers and can process asynchronous publishers concurrently,
while `concatMap()` processes them sequentially and preserves order.
Operator order matters.

### `switchIfEmpty()`

`switchIfEmpty()` switches to a fallback Publisher when the original
Publisher completes without emitting a value. If the original Publisher
emits a value, the fallback Publisher is not used.

``` text
Mono.empty()
    ↓
switchIfEmpty(fallback)
    ↓
fallback value
```

### `zip()`

`zip()` combines values emitted by multiple Publishers. With two `Mono`
instances, both must emit a value before a `Tuple2` can be created.

``` text
Mono<T1> ──┐
           ├── zip ──► Tuple2<T1, T2>
Mono<T2> ──┘
```

If one of the zipped Publishers completes empty, the zipped result also
completes without emitting a value. A fallback can be supplied before
the `zip()` when that matches the required semantics:

``` java
Mono<Integer> availablePlaces =
        Mono.<Integer>empty()
                .switchIfEmpty(Mono.just(0));
```

## 6. Reactive error handling

``` text
doOnError      → observe an error
onErrorReturn  → fixed fallback value
onErrorResume  → fallback Publisher
retry          → resubscribe upstream
timeout        → TimeoutException when work takes too long
```

`timeout().retry()` lets retry see the timeout; `retry().timeout()`
places the timeout downstream of retry.

## 7. StepVerifier

`StepVerifier` subscribes and verifies reactive signals without relying
on `Thread.sleep()`.

``` java
StepVerifier.create(result)
        .expectNext("SUCCESS")
        .verifyComplete();
```

## 8. Cold and hot publishers

``` text
COLD
subscription → new independent execution

HOT / SHARED
subscribers share an ongoing stream
late subscribers may miss previous emissions
```

We used `publish()` to create a `ConnectableFlux` and `connect()` to
start its shared upstream subscription.

## 9. Threading

A simple synchronous Reactor pipeline executes on the subscribing thread
by default.

``` text
Reactive != automatically multithreaded
```

`subscribeOn()` controls where subscription/upstream work is scheduled.
`publishOn()` changes the execution context for downstream operators
after that point.

``` text
subscribeOn → where subscription/upstream execution starts
publishOn   → from this point, downstream continues on another Scheduler
```

## 10. Reactor schedulers

``` text
Schedulers.parallel()
    → CPU-oriented work
    → avoid blocking

Schedulers.boundedElastic()
    → blocking / legacy work
    → isolate blocking operations
```

``` java
Mono.fromCallable(() -> oldBlockingLibrary.call())
        .subscribeOn(Schedulers.boundedElastic());
```

`boundedElastic()` does not make blocking code non-blocking; it isolates
that blocking work.

## 11. Event loop and blocking

The WebFlux event-loop model was introduced conceptually to understand
why blocking work is dangerous. With non-blocking I/O, an event-loop
thread can process other requests while waiting for I/O completion.

``` text
Event Loop + blocking operation = bad combination
```

When blocking code cannot be avoided:

``` java
Mono.fromCallable(() -> blockingOperation())
        .subscribeOn(Schedulers.boundedElastic());
```

The persistence distinction discussed conceptually was:

``` text
R2DBC → non-blocking → stays in reactive flow
JDBC  → blocking     → isolate if used from WebFlux
```

R2DBC has now been implemented for Event persistence in EventHub.

## 12. Current learning tests

`ReactorBasicsTest` currently covers Mono/Flux creation, Consumer and
signals, lazy execution, map/filter, flatMap/concatMap, `switchIfEmpty`,
`zip`, error propagation, retry/timeout/fallback, cold/hot publishers,
StepVerifier, default threading, subscribeOn, publishOn, parallel and
boundedElastic.

## 13. Spring WebFlux fundamentals

Spring WebFlux has now been added to EventHub. Project Reactor provides
the reactive programming model (`Mono`, `Flux`, operators, schedulers),
while Spring WebFlux provides the reactive HTTP/web layer.

The servlet-style and reactive models were compared conceptually:

``` text
Traditional blocking style
request → thread → blocking I/O → response

Reactive WebFlux style
request → event-loop processing → non-blocking I/O → continuation → response
```

Wrapping blocking JDBC work in `Mono.just(...)` does not make it
non-blocking because the JDBC call is evaluated before the `Mono` is
created. When unavoidable blocking work must be isolated, the studied
pattern is `fromCallable(...).subscribeOn(boundedElastic())`; the target
persistence stack for EventHub remains R2DBC.

## 14. Reactive application boundary

The EventHub application ports now expose reactive results:

``` text
CreateEventUseCase.createEvent(...) → Mono<Event>
EventQueryUseCase.findById(...)      → Mono<Event>
EventQueryUseCase.findAll()          → Flux<Event>

EventRepository.create(...)          → Mono<Event>
EventRepository.update(...)          → Mono<Event>
EventRepository.findById(...)        → Mono<Event>
EventRepository.findAll()            → Flux<Event>
```

The domain remains synchronous. `Event.create(...)` and domain behaviour
do not return `Mono` or `Flux`.

## 15. `fromSupplier` vs `defer` in the repository

The in-memory repository was used to reinforce lazy execution:

``` text
fromSupplier → execute lazily and produce a VALUE

defer        → execute lazily and produce/select a PUBLISHER
```

`Mono.justOrEmpty(...)` was also used to model a possibly absent value
without emitting `null`. Reactor publishers do not emit `null` values.

## 16. WebFlux HTTP input adapter

The first reactive HTTP input adapter is implemented with
`EventController`.

Current Event endpoints include:

``` text
POST /events
GET  /events
GET  /events/{id}
POST /events/{id}/publish
```

The Booking HTTP adapter now also exposes:

``` text
POST /bookings
```

The create flow is:

``` text
JSON
 ↓
CreateEventRequest
 ↓
CreateEventCommand
 ↓
CreateEventUseCase
 ↓
Mono<Event>
 ↓ map(Event → EventResponse)
Mono<EventResponse>
 ↓
JSON
```

`map()` is used because `Event → EventResponse` is a synchronous
transformation. The controller does not call `subscribe()`; WebFlux
subscribes at the HTTP boundary.

## 17. HTTP media types

The following HTTP concepts have been tested:

``` text
Content-Type → format of the body being sent
Accept       → format requested for the response
consumes     → media types accepted by the endpoint
produces     → media types produced by the endpoint
```

The create endpoint consumes and produces JSON and returns
`201 Created`. Sending an unsupported request `Content-Type` produced
`415 Unsupported Media Type`.

## 18. WebTestClient

`WebTestClient` is now used for Spring WebFlux HTTP integration tests.

The tests currently verify:

``` text
POST /events                       → 201 Created + JSON EventResponse
GET /events                        → 200 OK + JSON list
GET /events/{existing-id}          → 200 OK + JSON EventResponse
GET /events/{missing-valid-id}     → 404 Not Found
GET /events/{invalid-id-format}    → 400 Bad Request
```

The find-by-id test creates an Event first, captures the generated ID
from the POST response and uses it in the following GET request.

## 19. Empty publishers and HTTP not-found semantics

The unsuccessful query path has now been studied.
`EventRepository.findById(...)` represents absence with `Mono.empty()`.
An empty publisher is a completion signal, not an error signal, so it
does not automatically express HTTP `404 Not Found`.

The application service converts the empty result into an application
error:

``` text
Mono.empty()
    ↓
switchIfEmpty(Mono.error(new EventNotFoundException(...)))
    ↓
onError
```

The WebFlux exception handler then translates that application error to
HTTP 404. This keeps HTTP semantics out of the repository and
application exception itself.

## 20. WebFlux exception translation

`@RestControllerAdvice` provides shared exception handling for the web
layer, while `@ExceptionHandler` selects the exception type handled by
each method.

The mappings implemented and tested are:

``` text
EventNotFoundException   → 404 Not Found
InvalidEventIdException  → 400 Bad Request
```

The malformed-ID path is translated at the HTTP input adapter boundary.
`parseEventId(String)` converts the path variable to `EventId`; an
invalid UUID representation becomes `InvalidEventIdException`.

A generic handler for every `IllegalArgumentException` was tested and
then narrowed. Domain invariants also use `IllegalArgumentException`, so
globally translating the base exception to HTTP 400 would be too broad.

## 21. Integration-test state

The Spring `InMemoryEventRepository` is a singleton in the test
`ApplicationContext`, so its map may retain Events created by other
integration-test methods. The list test therefore does not assert that
exactly one Event exists or rely on list order. It captures the Event ID
created by the current POST, finds that Event in the GET result using
`filter(...).findFirst().orElseThrow()`, and verifies its fields.

This keeps the test focused on its actual requirement while leaving the
production repository scope unchanged.

## 22. Current position

``` text
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux fundamentals          ✅
        ↓
Reactive HTTP input adapter          ✅
        ↓
HTTP error handling                  ✅
        ↓
Request validation                   ✅
        ↓
Booking reactive flow / HTTP API     ✅
        ↓
Event R2DBC / PostgreSQL             ✅ CURRENT
```

## 23. Reactive coordination between Event and Booking

The Booking application flow has now provided a practical use of
`flatMap()` across application use cases.

``` text
reserveEventPlaces(...)
        ↓
Mono<Event>
        ↓ flatMap
bookingRepository.save(...)
        ↓
Mono<Booking>
```

The studied rule is:

``` text
lambda returns a normal value       → map
lambda returns Mono / Flux          → flatMap
ordered Flux async composition      → concatMap when sequential ordering is required
```

During implementation, an important lazy-composition mistake was tested
and corrected: calling a reactive repository method without
returning/composing its `Mono` means that operation is not part of the
subscribed pipeline.

`StepVerifier` is now also used in Booking application tests. The tests
verify a successful `Mono<Booking>` and verify an
`IllegalStateException` error signal when the requested number of places
exceeds Event capacity.

JUnit 5 `@BeforeEach` is used to recreate the stateful in-memory fake
repositories and application services before each test.

## 24. Booking HTTP API and controller isolation

The reactive Booking flow is now exposed through `POST /bookings`.

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
    ↓ map(Booking → BookingResponse)
    ↓
201 Created
```

The controller uses `map()` because `Booking → BookingResponse` is a
synchronous transformation.

A focused controller test was introduced with Mockito and
`WebTestClient`. `WebTestClient` is bound directly to the real
`BookingController`, while `CreateBookingUseCase` is mocked.

The Event publication flow also reinforces reactive composition:

``` text
EventRepository.findById(...)
    ↓
switchIfEmpty(...)
    ↓
flatMap(event → publish + save)
    ↓
Mono<Event>
```

The publish service is tested with `StepVerifier` for both successful
publication and `EventNotFoundException`.

## 25. Current position

``` text
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux fundamentals          ✅
        ↓
Reactive HTTP input adapters         ✅
        ↓
HTTP error handling                  ✅
        ↓
Request validation                   ✅
        ↓
Booking reactive application flow    ✅
        ↓
Booking HTTP API                     ✅
        ↓
Publish Event API                    ✅
        ↓
R2DBC / PostgreSQL                   ✅ EVENT PERSISTENCE
```

## 26. Spring Data R2DBC and PostgreSQL

Event persistence now stays reactive through Spring Data R2DBC and the PostgreSQL R2DBC driver.

``` text
WebFlux
  ↓
Application service
  ↓
EventRepository
  ↓
R2dbcEventRepositoryAdapter
  ↓
ReactiveCrudRepository
  ↓
R2DBC PostgreSQL driver
  ↓
PostgreSQL
```

Unlike wrapping JDBC in a `Mono`, R2DBC provides a non-blocking database access model that fits the reactive pipeline directly.

`SpringDataEventRepository` extends `ReactiveCrudRepository<EventEntity, UUID>`, so its operations already return `Mono` and `Flux`. The adapter maps those emitted persistence entities synchronously with `map()`:

``` text
Mono<EventEntity> → map(toDomain) → Mono<Event>
Flux<EventEntity> → map(toDomain) → Flux<Event>
```

No manual `subscribe()` is used in the repository adapter.

## 27. Reactive Create and Update

A generated UUID means a new Event already has a non-null ID before it reaches Spring Data. `EventEntity` therefore implements `Persistable<UUID>` so the adapter can explicitly distinguish new and existing persistence entities.

``` text
repository.create(event)
  ↓
isNew = true
  ↓
Spring Data save(...)
  ↓
INSERT

repository.update(event)
  ↓
isNew = false
  ↓
Spring Data save(...)
  ↓
UPDATE
```

The `isNew` value is marked `@Transient`: it controls Spring Data persistence behaviour but is not a PostgreSQL column.

The create and update paths were verified against the running PostgreSQL database: new Events are inserted as `DRAFT`, and publishing an Event updates the existing row to `PUBLISHED`.

The current Spring-backed HTTP integration tests use the configured PostgreSQL database, so tests that create Events can currently leave rows in the development database. PostgreSQL Testcontainers has not yet been implemented.

## 28. Current position

``` text
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux                      ✅
        ↓
HTTP / validation / Booking API     ✅
        ↓
Event R2DBC / PostgreSQL            ✅
        ↓
Booking R2DBC / PostgreSQL          ✅
        ↓
Concurrency / optimistic locking      🚧 CURRENT
```


## 29. Booking R2DBC persistence

Booking persistence now also reaches PostgreSQL through the reactive persistence stack.

``` text
POST /bookings
    ↓
CreateBookingService
    ↓
ReserveEventPlacesUseCase
    ↓
EventRepository.update(...)
    ↓
PostgreSQL: Event availability 20 → 17
    ↓
BookingRepository
    ↓
R2DBC / PostgreSQL
    ↓
Booking persisted
```

Both operations were manually verified against PostgreSQL. Reactive transaction management across the two writes has not yet been implemented or studied.

## 30. Concurrent reactive updates and optimistic locking

The Booking flow has now been tested with simultaneous operations that can
read the same Event state before either persistence update completes.

Reactive execution does not itself prevent this race:

``` text
Booking A ──► read Event version N ──► reserve ──► update
Booking B ──► read Event version N ──► reserve ──► update
```

Optimistic locking is now used at the R2DBC persistence boundary. The first
update succeeds and advances the Event version. A competing update based on
the stale version fails instead of silently overwriting the newer state.

``` text
first update   → version N matches → UPDATE succeeds → version N + 1
second update  → version N stale   → optimistic locking error
```

A focused concurrent-booking test reproduces this behaviour, and the complete
test suite is green.

The losing operation is now recovered by re-reading the Event and attempting
the reservation once more. The application reacts to a persistence-agnostic
`ConcurrentUpdateException`, while the R2DBC adapter owns translation from the
Spring Data optimistic-locking exception.

## 31. Sequencing verification with `then()` and `count()`

The final concurrency test introduced `then(...)` to sequence a verification
Publisher after the concurrent booking attempt.

``` text
concurrent booking attempt
        ↓
onErrorResume(...) for the expected insufficient-capacity result
        ↓
then(...)
        ↓
query persisted Bookings and Event
```

`then(otherPublisher)` waits for the upstream Publisher to complete successfully,
discards any value emitted by that upstream Publisher, and continues with the
Publisher supplied to `then`.

The Booking query returns `Flux<Booking>`. Calling `count()` produces a
`Mono<Long>`, which makes the number of persisted Bookings directly testable.

The final test combines:

``` text
Mono<Long>   Booking count
        +
Mono<Event>  persisted Event
        ↓
Mono.zip(...)
        ↓
Tuple2<Long, Event>
```

`StepVerifier` then verifies the final persisted business state: one Booking
exists and the Event has one available place.

Reactive transaction management across the Event update and Booking insert has
not yet been implemented or studied.
