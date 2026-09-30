package com.rubenmarin.eventhub.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Consumer;

class ReactorBasicsTest {

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

    @Test
    void shouldUnderstandConsumer() {

        Consumer<String> consumer =
                value -> System.out.println("Received: " + value);

        consumer.accept("EventHub");
    }

    @Test
    void shouldReceiveValueAndCompletionSignals() {

        Mono.just("EventHub")
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

    @Test
    void shouldCompleteWithoutValue() {

        Mono.<String>empty()
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }


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

    @Test
    void shouldEmitMultipleValuesWithFlux() {

        Flux.just("Climbing", "Running", "Cycling")
                .subscribe(
                        value -> System.out.println("onNext: " + value),
                        error -> System.out.println("onError: " + error.getMessage()),
                        () -> System.out.println("onComplete")
                );
    }

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

    @Test
    void shouldFilterValues() {

        Flux.just("Climbing", "Running", "Cycling")
                .filter(value -> value.length() > 7)
                .subscribe(
                        value -> System.out.println("onNext: " + value)
                );
    }

    @Test
    void shouldFilterAndTransformValues() {

        Flux.just("Climbing", "Run", "Cycling")
                .filter(value -> value.length() > 5)
                .map(value -> value.length())
                .subscribe(
                        value -> System.out.println("Received: " + value)
                );
    }

    @Test
    void shouldCreateNestedPublisherWithMap() {

        Flux<Mono<Integer>> result =
                Flux.just("Climbing", "Running", "Cycling")
                        .map(value -> Mono.just(value.length()));

        result.subscribe(
                value -> System.out.println("Received: " + value)
        );
    }

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

    @Test
    void shouldProcessConcurrentlyWithFlatMap() {

        Flux<String> result =
                Flux.just(
                                simulateAsyncOperation("First", 300),
                                simulateAsyncOperation("Second", 100),
                                simulateAsyncOperation("Third", 200)
                        )
                        .flatMap(mono -> mono);
//                .subscribe(
//                        value -> System.out.println("Received: " + value)
//                );

        StepVerifier.create(result)
                .expectNext("Second")
                .expectNext("Third")
                .expectNext("First")
                .verifyComplete();
    }

    @Test
    void shouldProcessConcurrentlyWithConcatMap() {

        Flux<String> result =
                Flux.just(
                                simulateAsyncOperation("First", 300),
                                simulateAsyncOperation("Second", 100),
                                simulateAsyncOperation("Third", 200)
                        )
                        .concatMap(mono -> mono);
//                .subscribe(
//                        value -> System.out.println("Received: " + value)
//                );

        StepVerifier.create(result)
                .expectNext("First")
                .expectNext("Second")
                .expectNext("Third")
                .verifyComplete();
    }
}