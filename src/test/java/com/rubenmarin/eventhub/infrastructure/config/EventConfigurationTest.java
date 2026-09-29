package com.rubenmarin.eventhub.infrastructure.config;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
 class EventConfigurationTest {

    @Autowired
    private CreateEventUseCase createEventUseCase;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldWireEventDependencies() {

        Assertions.assertNotNull(createEventUseCase);
        Assertions.assertNotNull(eventRepository);
    }
}
