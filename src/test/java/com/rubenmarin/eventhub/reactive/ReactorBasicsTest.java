package com.rubenmarin.eventhub.reactive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class ReactorBasicsTest {

    // Creates a Mono with one value and demonstrates different ways to subscribe to it.
    @Test
    void shouldCreateMono() {
        Mono<String> publisher = Mono.just("EventHub");
        publisher.subscribe(
                value -> System.out.println("Received: " + value)
        );
        Consumer<String> consumer =
                value -> System.out.println("Received2: " + value);
        publisher.subscribe(consumer);
    }

    // Shows how a Consumer receives a value through its accept() method.
    @Test
    void shouldUnderstandConsumer() {
        Consumer<String> consumer =
                value -> System.out.println("Received: " + value);
        consumer.accept("EventHub");
    }

    // Shows the onNext and onComplete signals of a successful Mono.
    @Test
    void shouldReceiveValueAndCompletionSignals() {
        Mono.just("EventHub")
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Shows that an empty Mono emits no value and only sends onComplete.
    @Test
    void shouldCompleteWithoutValue() {
        Mono.<String>empty()
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Shows that a failed Mono sends onError instead of onNext/onComplete.
    @Test
    void shouldCompleteWithError() {
        Mono.<String>error(
                        new IllegalStateException("Something went wrong")
                )
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Demonstrates lazy execution: the Supplier runs only when the Mono is subscribed to.
    @Test
    void shouldExecuteSupplierOnSubscription() {
        Mono<String> publisher =
                Mono.fromSupplier(() -> {
                    System.out.println("1. Supplier executed");
                    return "EventHub";
                });
        System.out.println("2. Mono created");
        publisher.subscribe(
                value -> System.out.println("3. Received: " + value)
        );
    }

    // Shows that Flux can emit multiple values before completing.
    @Test
    void shouldEmitMultipleValuesWithFlux() {
        Flux.just("Climbing", "Running", "Cycling")
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Uses map() to transform every emitted value while keeping the same type.
    @Test
    void shouldTransformValuesWithMap() {
        Flux.just("Climbing", "Running", "Cycling")
                .map(value -> value.toUpperCase())
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Shows that map() can also transform the emitted type, here String -> Integer.
    @Test
    void shouldTransformTypesWithMap() {
        Flux.just("Climbing", "Running", "Cycling")
                .map(value -> value.length())
                .subscribe(
                        value -> System.out.println("0onNext: " + value),
                        error -> System.out.println("0onError: " + error.getMessage()),
                        () -> System.out.println("0onComplete")
                );
        Flux<String> names = Flux.just("Climbing", "Running", "Cycling");
        Flux<Integer> lengths = names.map(value -> value.length());
        names.subscribe(
                value -> System.out.println("1onNext: " + value),
                error -> System.out.println("1onError: " + error.getMessage()),
                () -> System.out.println("1onComplete")
        );
        lengths.subscribe(
                value -> System.out.println("2onNext: " + value),
                error -> System.out.println("2onError: " + error.getMessage()),
                () -> System.out.println("2onComplete")
        );
    }

    // Demonstrates that building a reactive pipeline does not execute it without a subscription.
    @Test
    void shouldNotExecuteMapWithoutSubscription() {
        Flux<String> names =
                Flux.just("Climbing", "Running", "Cycling");
        Flux<Integer> lengths =
                names.map(value -> {
                    System.out.println("Mapping: " + value);
                    return value.length();
                });
        System.out.println("Pipeline created");
    }

    // Demonstrates that subscribing triggers the execution of the reactive pipeline.
    @Test
    void shouldExecuteMapOnSubscription() {
        Flux<String> names =
                Flux.just("Climbing", "Running", "Cycling");
        Flux<Integer> lengths =
                names.map(value -> {
                    System.out.println("Mapping: " + value);
                    return value.length();
                });
        System.out.println("Pipeline created");
        lengths.subscribe(
                length -> System.out.println("onNext: " + length),
                error -> System.out.println("onError: " + error.getMessage()),
                () -> System.out.println("onComplete")
        );
    }

    // Uses filter() to allow only values matching a condition to continue downstream.
    @Test
    void shouldFilterValues() {
        Flux.just("Climbing", "Running", "Cycling")
                .filter(value -> value.length() > 7)
                .subscribe(
                        value -> System.out.println("onNext: " + value)
                );
    }

    // Shows how operators can be chained: filter first, then transform with map().
    @Test
    void shouldFilterAndTransformValues() {
        Flux.just("Climbing", "Run", "Cycling")
                .filter(value -> value.length() > 5)
                .map(value -> value.length())
                .subscribe(
                        value -> System.out.println("Received: " + value)
                );
    }

    // Shows that map() with an async operation creates nested publishers: Flux<Mono<T>>.
    @Test
    void shouldCreateNestedPublisherWithMap() {
        Flux<Mono<Integer>> result =
                Flux.just("Climbing", "Running", "Cycling")
                        .map(value -> Mono.just(value.length()));
        result.subscribe(
                value -> System.out.println("Received: " + value)
        );
    }

    // Uses flatMap() to flatten nested publishers into a single reactive sequence.
    @Test
    void shouldFlattenPublisherWithFlatMap() {
        Flux<Integer> result =
                Flux.just("Climbing", "Running", "Cycling")
                        .flatMap(
                                value -> Mono.just(value.length())
                        );
        result.subscribe(
                value -> System.out.println("Received: " + value)
        );
    }

    private Mono<String> simulateAsyncOperation(
            String value,
            long delayMillis
    ) {
        return Mono.just(value)
                .delayElement(Duration.ofMillis(delayMillis))
                .doOnNext(result -> System.out.println("onNext: " + result));
    }

    // Shows that flatMap() can process async publishers concurrently, so completion order may differ.
    @Test
    void shouldProcessConcurrentlyWithFlatMap() {
        Flux<String> result =
                Flux.just(
                                simulateAsyncOperation("First", 300),
                                simulateAsyncOperation("Second", 100),
                                simulateAsyncOperation("Third", 200)
                        )
                        .flatMap(mono -> mono);
        StepVerifier.create(result)
                .expectNext("Second")
                .expectNext("Third")
                .expectNext("First")
                .verifyComplete();
    }

    // Shows that concatMap() processes publishers sequentially and preserves their order.
    @Test
    void shouldProcessConcurrentlyWithConcatMap() {
        Flux<String> result =
                Flux.just(
                                simulateAsyncOperation("First", 300),
                                simulateAsyncOperation("Second", 100),
                                simulateAsyncOperation("Third", 200)
                        )
                        .concatMap(mono -> mono);
        StepVerifier.create(result)
                .expectNext("First")
                .expectNext("Second")
                .expectNext("Third")
                .verifyComplete();
    }

    // Shows that an exception becomes an onError signal that can be observed or recovered.
    @Test
    void shouldConvertExceptionIntoErrorSignal() {
        Flux.just("Climbing", "ERROR", "Cycling")
                .map(value -> {
                    System.out.println("Processing: " + value);
                    if (value.equals("ERROR")) {
                        throw new IllegalStateException("Cannot process value");
                    }
                    return value.toUpperCase();
                })
                .doOnError(error ->
                        System.out.println("doOnError: " + error.getMessage()))
                //.onErrorResume(error -> Flux.just("FALLBACK"))
                .onErrorReturn("FALLBACK")
                .subscribe(
                        value -> System.out.println("Subscribe onNext: " + value),
                        error -> System.out.println("Subscribe onError: " + error.getMessage()),
                        () -> System.out.println("Subscribe onComplete")
                );
    }

    // Shows that retry() resubscribes to the upstream publisher after an error.
    @Test
    void shouldRetryAfterError() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> operation =
                Mono.fromSupplier(() -> {
                    int attempt = attempts.incrementAndGet();
                    System.out.println("Executing attempt: " + attempt);
                    if (attempt < 3) {
                        System.err.println("Temporary error on attempt: " + attempt);
                        throw new IllegalStateException("Temporary error");
                    }
                    return "SUCCESS";
                });
        operation
                .retry(1)
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    // Shows that timeout() emits a TimeoutException when an operation takes too long.
    @Test
    void shouldTimeoutWhenOperationTakesTooLong() {
        Mono<String> operation =
                Mono.just("SUCCESS")
                        .delayElement(Duration.ofSeconds(2));
        Mono<String> result =
                operation.timeout(Duration.ofMillis(500));
        StepVerifier.create(result)
                .expectError(TimeoutException.class)
                .verify();
    }

    // Shows that timeout() allows the value through when the operation completes in time.
    @Test
    void shouldNotTimeoutWhenOperationCompletesInTime() {
        Mono<String> operation =
                Mono.just("SUCCESS")
                        .delayElement(Duration.ofMillis(100));
        Mono<String> result =
                operation.timeout(Duration.ofMillis(500));
        StepVerifier.create(result)
                .expectNext("SUCCESS")
                .verifyComplete();
    }

    // Shows that timeout() before retry() makes every timeout trigger a new subscription attempt.
    @Test
    void shouldRetryAfterTimeout() {

        AtomicInteger attempts = new AtomicInteger();

        Mono<String> operation =
                Mono.defer(() -> {
                    int attempt = attempts.incrementAndGet();
                    System.out.println("Starting attempt: " + attempt);
                    return Mono.just("SUCCESS")
                            .delayElement(Duration.ofMillis(300));
                });

        Mono<String> result =
                operation
                        .timeout(Duration.ofMillis(100))
                        .retry(2);

        StepVerifier.create(result)
                .expectError(TimeoutException.class)
                .verify();

        System.out.println("Total attempts result: " + attempts.get());

        Assertions.assertEquals(3, attempts.get());
    }

    // Shows that retry() cannot retry a downstream timeout because it never receives that error.
    @Test
    void shouldTimeoutWithNoRetry() {

        AtomicInteger attempts = new AtomicInteger();

        Mono<String> operation =
                Mono.defer(() -> {
                    int attempt = attempts.incrementAndGet();
                    System.out.println("Starting attempt: " + attempt);
                    return Mono.just("SUCCESS")
                            .delayElement(Duration.ofMillis(300));
                });

        Mono<String> result =
                operation
                        .retry(2)
                        .timeout(Duration.ofMillis(100));

        StepVerifier.create(result)
                .expectError(TimeoutException.class)
                .verify();

        System.out.println("Total attempts result: " + attempts.get());

        Assertions.assertEquals(1, attempts.get());
    }

    // Combines timeout, retry and onErrorResume to provide a fallback after retries fail.
    @Test
    void shouldFallbackAfterTimeoutRetriesAreExhausted() {

        AtomicInteger attempts = new AtomicInteger();

        Mono<String> operation =
                Mono.defer(() -> {
                    int attempt = attempts.incrementAndGet();
                    System.out.println("Starting attempt: " + attempt);
                    return Mono.just("SUCCESS")
                            .delayElement(Duration.ofMillis(300));
                });

        Mono<String> result =
                operation
                        .timeout(Duration.ofMillis(100))
                        .retry(2)
                        .onErrorResume(TimeoutException.class, error -> {
                            System.out.println("Retries exhausted. Using fallback.");
                            return Mono.just("FALLBACK");
                        });

        StepVerifier.create(result)
                .expectNext("FALLBACK")
                .verifyComplete();

        Assertions.assertEquals(3, attempts.get());
    }

    // Demonstrates a cold publisher: every subscriber triggers a new independent execution.
    @Test
    void shouldExecuteColdPublisherForEachSubscriber() {
        AtomicInteger executions = new AtomicInteger();

        Mono<String> coldPublisher =
                Mono.defer(() -> {
                    int execution = executions.incrementAndGet();
                    System.out.println("Executing publisher: " + execution);
                    return Mono.just("EventHub-" + execution);
                });

        coldPublisher.subscribe(value -> System.out.println("Subscriber 1: " + value));

        coldPublisher.subscribe(value -> System.out.println("Subscriber 2: " + value));

        System.out.println("Total executions: " + executions.get());
    }

    // Demonstrates a shared/hot publisher where subscribers share one upstream execution.
    @Test
    void shouldShareOneExecutionBetweenSubscribers() {
        AtomicInteger executions = new AtomicInteger();

        Flux<Integer> coldPublisher =
                Flux.defer(() -> {
                    int execution = executions.incrementAndGet();
                    System.out.println("Starting execution: " + execution);
                    return Flux.just(1, 2, 3);
                });

        ConnectableFlux<Integer> hotPublisher = coldPublisher.publish();

        List<Integer> subscriber1 = new ArrayList<>();
        List<Integer> subscriber2 = new ArrayList<>();

        hotPublisher.subscribe(value -> {
            subscriber1.add(value);
            System.out.println("subscriber1 value: " + value);
        });

        hotPublisher.subscribe(value -> {
            subscriber2.add(value);
            System.out.println("subscriber2 value: " + value);
        });

        System.out.println("Total executions before connect: " + executions.get());

        hotPublisher.connect();

        Assertions.assertEquals(List.of(1, 2, 3), subscriber1);
        Assertions.assertEquals(List.of(1, 2, 3), subscriber2);
        Assertions.assertEquals(1, executions.get());

        System.out.println("Total executions: " + executions.get());
    }

    // Shows that a late subscriber to a hot publisher misses values emitted before it subscribed.
    @Test
    void shouldMissPreviousValuesWhenSubscribingLateToHotPublisher() {

        ConnectableFlux<Long> hotPublisher =
                Flux.interval(Duration.ofMillis(100))
                        .take(5)
                        .publish();

        List<Long> subscriber2 = new ArrayList<>();

        Mono<Long> publisher = Mono.delay(Duration.ofMillis(250));

        publisher.subscribe(ignored -> {
            System.out.println("Subscriber 2 joining");
            hotPublisher.subscribe(value -> subscriber2.add(value));
        });

        StepVerifier.create(hotPublisher)
                .then(() -> hotPublisher.connect())
                .expectNext(0L, 1L, 2L, 3L, 4L)
                .verifyComplete();

        System.out.println("Subscriber 2 received: " + subscriber2);

        Assertions.assertEquals(List.of(2L, 3L, 4L), subscriber2);
    }

    // Shows that Reactor executes synchronously on the subscribing thread by default.
    @Test
    void shouldExecuteOnCurrentThreadByDefault() {

        System.out.println("Before subscribe: " + Thread.currentThread().getName());

        Mono<String> publisher = Mono.just("EventHub")
                .map(value -> {
                    System.out.println(
                            "Value: " + value + " | map: " + Thread.currentThread().getName()
                    );
                    return value.toUpperCase();
                });

        publisher.subscribe(value ->
                System.out.println(
                        "Value: " + value + " | subscriber: " + Thread.currentThread().getName()
                )
        );

        System.out.println("After subscribe: " + Thread.currentThread().getName());
    }

    // Shows that subscribeOn() schedules subscription/upstream execution on another Scheduler.
    @Test
    void shouldExecuteOnSubscribeOnScheduler() {

        System.out.println("Before subscribe: " + Thread.currentThread().getName());

        Mono<String> publisher =
                Mono.just("EventHub")

                        .map(value -> {
                            System.out.println(
                                    "Value: " + value + " | map: " + Thread.currentThread().getName()
                            );
                            return value.toUpperCase();
                        })

                        .subscribeOn(Schedulers.boundedElastic());

        publisher.subscribe(value ->
                System.out.println(
                        "Value: " + value + " | subscriber: " + Thread.currentThread().getName()
                )
        );

        System.out.println("After subscribe: " + Thread.currentThread().getName());
    }

    // Shows that publishOn() switches the execution thread for downstream operators after it.
    @Test
    void shouldSwitchThreadWithPublishOn() {

        System.out.println("Before subscribe: " + Thread.currentThread().getName());

        Mono<String> publisher = Mono.just("EventHub")
                .map(value -> {
                    System.out.println(
                            "Map 1: " + value + " | thread: " + Thread.currentThread().getName()
                    );
                    return value.toUpperCase();
                })

                .publishOn(Schedulers.boundedElastic())

                .map(value -> {
                    System.out.println(
                            "Map 2: " + value + " | thread: " + Thread.currentThread().getName()
                    );
                    return value + " REACTIVE";
                });

        publisher.subscribe(value ->
                System.out.println(
                        "Subscriber: " + value + " | thread: " + Thread.currentThread().getName()
                )
        );

        System.out.println("After subscribe: " + Thread.currentThread().getName());
    }

    // Combines subscribeOn() and publishOn() to show upstream and downstream thread switching.
    @Test
    void shouldUseSubscribeOnAndPublishOnTogether() {

        System.out.println("Before subscribe: " + Thread.currentThread().getName());

        Mono<String> publisher = Mono.just("EventHub")
                .map(value -> {
                    System.out.println("Map 1: " + Thread.currentThread().getName());
                    return value.toUpperCase();
                })

                .publishOn(Schedulers.parallel())

                .map(value -> {
                    System.out.println("Map 2: " + Thread.currentThread().getName());
                    return value + " REACTIVE";
                })

                .subscribeOn(Schedulers.boundedElastic());

        publisher.subscribe(value ->
                System.out.println(
                        "Value: " + value + " | Subscriber: " + Thread.currentThread().getName()
                )
        );

        System.out.println("After subscribe: " + Thread.currentThread().getName());
    }

    // Verifies that switchIfEmpty provides a fallback value only when
    // the original publisher completes without emitting a value.
    @Test
    void shouldSwitchToFallbackWhenPublisherIsEmpty() {

        Mono<String> emptyPublisher =
                Mono.<String>empty()
                        .switchIfEmpty(Mono.just("Event not found"));

        Mono<String> notEmptyPublisher =
                Mono.just("EventHub")
                        .switchIfEmpty(Mono.just("Event not found"));

        StepVerifier.create(emptyPublisher)
                .expectNext("Event not found")
                .verifyComplete();

        StepVerifier.create(notEmptyPublisher)
                .expectNext("EventHub")
                .verifyComplete();
    }


    // Verifies that zip waits for both publishers and combines their emitted values into a Tuple2.
    @Test
    void shouldCombinePublishersWithZip() {

        Mono<String> event = Mono.just("Climbing Workshop");

        Mono<Integer> availablePlaces = Mono.just(12);

        Mono<String> result =
                Mono.zip(event, availablePlaces)
                        .map(tuple ->
                                tuple.getT1() + " - " + tuple.getT2() + " places available"
                        );

        StepVerifier.create(result)
                .expectNext("Climbing Workshop - 12 places available")
                .verifyComplete();
    }


    // Verifies that zip completes empty when one of its publishers completes without emitting a value.
    @Test
    void shouldCompleteEmptyWhenOneZippedPublisherIsEmpty() {

        Mono<String> event = Mono.just("Climbing Workshop");

        Mono<Integer> availablePlaces = Mono.empty();

        Mono<String> result =
                Mono.zip(event, availablePlaces)
                        .map(tuple ->
                                tuple.getT1() + " - " + tuple.getT2() + " places available"
                        );

        result.subscribe(
                value -> System.out.println("onNext: " + value),
                error -> System.out.println("onError: " + error),
                () -> System.out.println("onComplete")
        );

        StepVerifier.create(result)
                .verifyComplete();
    }

    // Verifies that switchIfEmpty can provide the missing value
    // required by zip to combine both publishers.
    @Test
    void shouldCombinePublishersWhenEmptyPublisherHasFallback() {

        Mono<String> event = Mono.just("Climbing Workshop");

        Mono<Integer> availablePlaces =
                Mono.<Integer>empty()
                        .switchIfEmpty(Mono.just(0));

        Mono<String> result =
                Mono.zip(event, availablePlaces)
                        .map(tuple ->
                                tuple.getT1() + " - " + tuple.getT2() + " places available"
                        );

        result.subscribe(
                value -> System.out.println("onNext: " + value),
                error -> System.out.println("onError: " + error),
                () -> System.out.println("onComplete")
        );

        StepVerifier.create(result)
                .expectNext("Climbing Workshop - 0 places available")
                .verifyComplete();
    }
}