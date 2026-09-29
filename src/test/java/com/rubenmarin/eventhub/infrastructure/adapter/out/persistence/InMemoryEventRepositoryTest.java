package com.rubenmarin.eventhub.infrastructure.adapter.out.persistence;


import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Capacity;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventName;
import com.rubenmarin.eventhub.event.domain.model.Money;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.InMemoryEventRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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


        Event savedEvent =  repository.save(event);

        Assertions.assertSame(event, savedEvent);

    }

}