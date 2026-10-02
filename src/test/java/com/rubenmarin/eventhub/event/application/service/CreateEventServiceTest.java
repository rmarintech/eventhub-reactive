package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

public class CreateEventServiceTest {


    @Test
    void shouldCreateEvent() {

        InMemoryEventRepository repository = new InMemoryEventRepository();
        CreateEventService service = new CreateEventService(repository);


        CreateEventCommand command =
                new CreateEventCommand(
                        "Reactive Java Workshop",
                        "Introduction to Project Reactor",
                        LocalDateTime.now().plusDays(30),
                        20,
                        new BigDecimal("49.99"),
                        Currency.getInstance("EUR")
                );


//        Event created = service.createEvent(command);
//
//        Assertions.assertNotNull(created.id());
//        Assertions.assertEquals(new EventName("Reactive Java Workshop"), created.name());
//        Assertions.assertEquals("Introduction to Project Reactor", created.description());
//        Assertions.assertNotNull(created.startDate());
//        Assertions.assertEquals(20, created.capacity().total());
//        Assertions.assertEquals(20, created.capacity().available());
//        Assertions.assertEquals(Money.euros(new BigDecimal("49.99")), created.price());
//        Assertions.assertEquals(EventStatus.DRAFT, created.status());
//
//        Assertions.assertEquals(created, repository.savedEvent);


        Mono<Event> created = service.createEvent(command);

        StepVerifier.create(created)
                .assertNext(event -> {
                    Assertions.assertNotNull(event.id());
                    Assertions.assertEquals(new EventName("Reactive Java Workshop"), event.name());
                    Assertions.assertEquals("Introduction to Project Reactor", event.description());
                    Assertions.assertNotNull(event.startDate());
                    Assertions.assertEquals(20, event.capacity().total());
                    Assertions.assertEquals(20, event.capacity().available());
                    Assertions.assertEquals(Money.euros(new BigDecimal("49.99")), event.price());
                    Assertions.assertEquals(EventStatus.DRAFT, event.status());
                    Assertions.assertEquals(event, repository.savedEvent);
                })
                .verifyComplete();


    }


    private static class InMemoryEventRepository implements EventRepository {

        private Event savedEvent;

        @Override
        public Mono<Event> save(Event event) {

           // return Mono.defer(() -> Mono.just(this.savedEvent = event));

            return Mono.fromSupplier(() -> {
                this.savedEvent= event;
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
