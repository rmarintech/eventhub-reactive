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

## 13. Current position

```text
Reactive Programming Fundamentals   ✅
        ↓
Spring WebFlux                      🚧 NEXT
        ↓
Reactive HTTP input adapter         ⏳
        ↓
R2DBC / PostgreSQL                  ⏳
```

The next step is to introduce Spring WebFlux and connect the first real HTTP input adapter to the existing EventHub application boundary.
