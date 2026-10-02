package com.rubenmarin.eventhub.infrastructure.adapter.out.persistence;


import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Capacity;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventName;
import com.rubenmarin.eventhub.event.domain.model.Money;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.InMemoryEventRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class InMemoryEventRepositoryTest {

    @Test
    void shouldSaveEvent() {
        Event event = Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                Money.euros(new BigDecimal("49.99"))

        );
        InMemoryEventRepository repository = new InMemoryEventRepository();


        Mono<Event> savedEvent =  repository.save(event);

//        List< Event> events = new ArrayList<>();
//        savedEvent.subscribe( value -> events.add(value) );
//         Assertions.assertSame(event, events.getFirst());


        StepVerifier.create(savedEvent)
                .expectNext(event)
                .verifyComplete();




    }

}