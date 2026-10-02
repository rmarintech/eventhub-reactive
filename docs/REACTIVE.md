# Reactive Programming --- EventHub

This document contains only the Reactive Programming concepts already studied and tested in EventHub.

## 1. Core model

We studied **imperative vs reactive**, **blocking vs non-blocking**, and **synchronous vs asynchronous**. Reactive programming models work as streams of signals; it does not automatically mean asynchronous or multithreaded execution.

## 2. Reactive Streams

The model studied contains `Publisher`, `Subscriber`, `Subscription` and `Processor`. Backpressure lets a subscriber express how much data it is ready to receive. Project Reactor implements this model for Java.

## 3. Mono and Flux

```text
Mono<T> → 0..1 values
Flux<T> → 0..N values
```

Signals studied are `onNext`, `onComplete` and `onError`. `onError` and `onComplete` are terminal signals.

## 4. Subscription and lazy execution

Building a pipeline does not necessarily execute it. Subscription triggers execution.

```text
build pipeline → recipe exists → subscribe → execution starts
```

Studied creation methods:

```text
Mono.just(value)       → value already available
Mono.fromSupplier(...) → lazy value
Mono.fromCallable(...) → lazy Callable-based operation
Mono.defer(...)        → lazy Publisher creation
```

## 5. Operators

`map()` transforms values, `filter()` filters them, `flatMap()` flattens nested publishers and can process asynchronous publishers concurrently, while `concatMap()` processes them sequentially and preserves order. Operator order matters.

### `switchIfEmpty()`

`switchIfEmpty()` switches to a fallback Publisher when the original Publisher completes without emitting a value. If the original Publisher emits a value, the fallback Publisher is not used.

```text
Mono.empty()
    ↓
switchIfEmpty(fallback)
    ↓
fallback value
```

### `zip()`

`zip()` combines values emitted by multiple Publishers. With two `Mono` instances, both must emit a value before a `Tuple2` can be created.

```text
Mono<T1> ──┐
           ├── zip ──► Tuple2<T1, T2>
Mono<T2> ──┘
```

If one of the zipped Publishers completes empty, the zipped result also completes without emitting a value. A fallback can be supplied before the `zip()` when that matches the required semantics:

```java
Mono<Integer> availablePlaces =
        Mono.<Integer>empty()
                .switchIfEmpty(Mono.just(0));
```

## 6. Reactive error handling

```text
doOnError      → observe an error
onErrorReturn  → fixed fallback value
onErrorResume  → fallback Publisher
retry          → resubscribe upstream
timeout        → TimeoutException when work takes too long
```

`timeout().retry()` lets retry see the timeout; `retry().timeout()` places the timeout downstream of retry.

## 7. StepVerifier

`StepVerifier` subscribes and verifies reactive signals without relying on `Thread.sleep()`.

```java
StepVerifier.create(result)
        .expectNext("SUCCESS")
        .verifyComplete();
```

## 8. Cold and hot publishers

```text
COLD
subscription → new independent execution

HOT / SHARED
subscribers share an ongoing stream
late subscribers may miss previous emissions
```

We used `publish()` to create a `ConnectableFlux` and `connect()` to start its shared upstream subscription.

## 9. Threading

A simple synchronous Reactor pipeline executes on the subscribing thread by default.

```text
Reactive != automatically multithreaded
```

`subscribeOn()` controls where subscription/upstream work is scheduled. `publishOn()` changes the execution context for downstream operators after that point.

```text
subscribeOn → where subscription/upstream execution starts
publishOn   → from this point, downstream continues on another Scheduler
```

## 10. Reactor schedulers

```text
Schedulers.parallel()
    → CPU-oriented work
    → avoid blocking

Schedulers.boundedElastic()
    → blocking / legacy work
    → isolate blocking operations
```

```java
Mono.fromCallable(() -> oldBlockingLibrary.call())
        .subscribeOn(Schedulers.boundedElastic());
```

`boundedElastic()` does not make blocking code non-blocking; it isolates that blocking work.

## 11. Event loop and blocking

The WebFlux event-loop model was introduced conceptually to understand why blocking work is dangerous. With non-blocking I/O, an event-loop thread can process other requests while waiting for I/O completion.

```text
Event Loop + blocking operation = bad combination
```

When blocking code cannot be avoided:

```java
Mono.fromCallable(() -> blockingOperation())
        .subscribeOn(Schedulers.boundedElastic());
```

The persistence distinction discussed conceptually was:

```text
R2DBC → non-blocking → stays in reactive flow
JDBC  → blocking     → isolate if used from WebFlux
```

R2DBC itself has not yet been implemented in EventHub.

## 12. Current learning tests

`ReactorBasicsTest` currently covers Mono/Flux creation, Consumer and signals, lazy execution, map/filter, flatMap/concatMap, `switchIfEmpty`, `zip`, error propagation, retry/timeout/fallback, cold/hot publishers, StepVerifier, default threading, subscribeOn, publishOn, parallel and boundedElastic.

## 13. Spring WebFlux fundamentals

Spring WebFlux has now been added to EventHub. Project Reactor provides the reactive programming model (`Mono`, `Flux`, operators, schedulers), while Spring WebFlux provides the reactive HTTP/web layer.

The servlet-style and reactive models were compared conceptually:

```text
Traditional blocking style
request → thread → blocking I/O → response

Reactive WebFlux style
request → event-loop processing → non-blocking I/O → continuation → response
```

Wrapping blocking JDBC work in `Mono.just(...)` does not make it non-blocking because the JDBC call is evaluated before the `Mono` is created. When unavoidable blocking work must be isolated, the studied pattern is `fromCallable(...).subscribeOn(boundedElastic())`; the target persistence stack for EventHub remains R2DBC.

## 14. Reactive application boundary

The EventHub application ports now expose reactive results:

```text
CreateEventUseCase.createEvent(...) → Mono<Event>
EventQueryUseCase.findById(...)      → Mono<Event>
EventQueryUseCase.findAll()          → Flux<Event>

EventRepository.save(...)            → Mono<Event>
EventRepository.findById(...)        → Mono<Event>
EventRepository.findAll()            → Flux<Event>
```

The domain remains synchronous. `Event.create(...)` and domain behaviour do not return `Mono` or `Flux`.

## 15. `fromSupplier` vs `defer` in the repository

The in-memory repository was used to reinforce lazy execution:

```text
fromSupplier → execute lazily and produce a VALUE

defer        → execute lazily and produce/select a PUBLISHER
```

`Mono.justOrEmpty(...)` was also used to model a possibly absent value without emitting `null`. Reactor publishers do not emit `null` values.

## 16. WebFlux HTTP input adapter

The first reactive HTTP input adapter is implemented with `EventController`.

Current endpoints:

```text
POST /events
GET  /events
GET  /events/{id}
```

The create flow is:

```text
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

`map()` is used because `Event → EventResponse` is a synchronous transformation. The controller does not call `subscribe()`; WebFlux subscribes at the HTTP boundary.

## 17. HTTP media types

The following HTTP concepts have been tested:

```text
Content-Type → format of the body being sent
Accept       → format requested for the response
consumes     → media types accepted by the endpoint
produces     → media types produced by the endpoint
```

The create endpoint consumes and produces JSON and returns `201 Created`. Sending an unsupported request `Content-Type` produced `415 Unsupported Media Type`.

## 18. WebTestClient

`WebTestClient` is now used for Spring WebFlux HTTP integration tests.

The tests currently verify:

```text
POST /events       → 201 Created + JSON EventResponse
GET /events        → 200 OK + JSON list
GET /events/{id}   → 200 OK + JSON EventResponse
```

The find-by-id test creates an Event first, captures the generated ID from the POST response and uses it in the following GET request.

## 19. Current position

```text
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux fundamentals          ✅ CURRENT
        ↓
Reactive HTTP input adapter          ✅
        ↓
HTTP error handling / validation     🚧 NEXT
        ↓
R2DBC / PostgreSQL                   ⏳
```

The next step is to study the unsuccessful HTTP paths, starting with an existing UUID that does not identify an Event and the mapping of an empty `Mono` to an appropriate HTTP response.
