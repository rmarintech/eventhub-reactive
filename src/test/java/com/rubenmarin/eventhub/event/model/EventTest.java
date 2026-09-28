package com.rubenmarin.eventhub.event.model;

import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventTest {

    private Event createEvent() {

        return Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                Money.euros(new BigDecimal("49.99"))
        );
    }

    @Test
    void shouldCreateEventAsDraft() {
        Event event = createEvent();

        Assertions.assertNotNull(event.id());
        Assertions.assertEquals(EventStatus.DRAFT, event.status());
    }

    @Test
    void shouldPublishDraftEvent() {
        Event event = createEvent();

        event.publish();

        Assertions.assertEquals(EventStatus.PUBLISHED, event.status());
    }

    @Test
    void shouldNotReservePlacesForDraftEvent() {

        Event event = createEvent();
        Assertions.assertThrows(IllegalStateException.class,  () ->  event.reservePlaces(2));
    }

    @Test
    void shouldReservePlacesForPublishedEvent() {
        Event event = createEvent();
        event.publish();
        event.reservePlaces(2);

        Assertions.assertEquals(EventStatus.PUBLISHED, event.status());
        Assertions.assertEquals(18, event.capacity().available());
    }

    @Test
    void shouldRejectReservationWhenCapacityIsInsufficient() {

        Event event = createEvent();
        event.publish();

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> event.reservePlaces(21)
        );
    }
}
