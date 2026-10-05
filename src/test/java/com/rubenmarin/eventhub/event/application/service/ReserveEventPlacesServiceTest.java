package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
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

public class ReserveEventPlacesServiceTest {


    @Test
    void shouldReserveEventPlaces() {

        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        ReserveEventPlacesService reserveEventPlacesService = new ReserveEventPlacesService(eventRepository);


        Event newEvent = Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                new Money(new BigDecimal("49.99"), Currency.getInstance("EUR"))

        );

        newEvent.publish();

        Mono<Event> savedEvent = eventRepository.update(newEvent);


        Mono<Event> reservedEvent = savedEvent.flatMap(event -> {
            return reserveEventPlacesService.reserveEventPlaces(event.id(), 3);
        });


        StepVerifier.create(reservedEvent)
                .assertNext(event -> {
                    Assertions.assertNotNull(event.id());
                    Assertions.assertEquals(20, event.capacity().total());
                    Assertions.assertEquals(17, event.capacity().available());
                    Assertions.assertEquals(EventStatus.PUBLISHED, event.status());
                })
                .verifyComplete();


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
