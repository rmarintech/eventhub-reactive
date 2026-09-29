package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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


        Event created = service.createEvent(command);

        Assertions.assertNotNull(created.id());
        Assertions.assertEquals(new EventName("Reactive Java Workshop"), created.name());
        Assertions.assertEquals("Introduction to Project Reactor", created.description());
        Assertions.assertNotNull(created.startDate());
        Assertions.assertEquals(20, created.capacity().total());
        Assertions.assertEquals(20, created.capacity().available());
        Assertions.assertEquals(Money.euros(new BigDecimal("49.99")), created.price());
        Assertions.assertEquals(EventStatus.DRAFT, created.status());

        Assertions.assertEquals(created, repository.savedEvent);
    }


    private static class InMemoryEventRepository
            implements EventRepository {

        private Event savedEvent;

        @Override
        public Event save(Event event) {
            this.savedEvent = event;
            return event;
        }
    }

}
