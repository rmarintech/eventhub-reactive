package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

public class PublishEventServiceTest {


    @Test
    void shouldPublishEvent() {

        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        PublishEventService publishEventService  = new PublishEventService(eventRepository);

        Event newEvent = Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                new Money(new BigDecimal("49.99"), Currency.getInstance("EUR"))

        );

        Mono<Event> savedEvent = eventRepository.update(newEvent);

        Mono<Event> publishedEvent =savedEvent.flatMap(event -> {

            return publishEventService.publishEvent(event.id());
        });

                StepVerifier.create(publishedEvent)
                .assertNext(event -> {
                    Assertions.assertNotNull(event.id());
                    Assertions.assertEquals(newEvent.id(), event.id());
                    Assertions.assertEquals(EventStatus.PUBLISHED, event.status());
                })
                .verifyComplete();

    }

    @Test
    void shouldFailWhenPublishingEventNotFound() {

        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        PublishEventService publishEventService  = new PublishEventService(eventRepository);


       Mono<Event> publishedEvent = publishEventService.publishEvent(EventId.generate());


        StepVerifier.create(publishedEvent)
                .expectError(EventNotFoundException.class)
                .verify();

    }


    private static class InMemoryEventRepository implements EventRepository {

        private Event savedEvent;

        @Override
        public Mono<Event> create(Event event) {

            // return Mono.defer(() -> Mono.just(this.savedEvent = event));

            return Mono.fromSupplier(() -> {
                this.savedEvent = event;
                return event;
            });
        }

        @Override
        public Mono<Event> update(Event event) {

            // return Mono.defer(() -> Mono.just(this.savedEvent = event));

            return Mono.fromSupplier(() -> {
                this.savedEvent = event;
                return event;
            });
        }

        @Override
        public Mono<Event> findById(EventId id) {
            return Mono.defer(() -> {
                        if (savedEvent == null || !savedEvent.id().equals(id)) {
                            return Mono.empty();
                        } else {
                            return Mono.just(savedEvent);
                        }
                    }
            );
        }

        @Override
        public Flux<Event> findAll() {
            return Flux.defer(() -> Mono.justOrEmpty(savedEvent));
        }
    }

}
