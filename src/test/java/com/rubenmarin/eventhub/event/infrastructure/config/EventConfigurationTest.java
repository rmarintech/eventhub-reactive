package com.rubenmarin.eventhub.event.infrastructure.config;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.application.port.in.PublishEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
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

    @Autowired
    private PublishEventUseCase publishEventUseCase;

    @Autowired
    private EventQueryUseCase eventQueryUseCase;


    @Autowired
    private ReserveEventPlacesUseCase reserveEventPlacesUseCase;

    @Test
    void shouldWireEventDependencies() {

        Assertions.assertNotNull(createEventUseCase);
        Assertions.assertNotNull(eventRepository);
        Assertions.assertNotNull(publishEventUseCase);
        Assertions.assertNotNull(eventQueryUseCase);
        Assertions.assertNotNull(reserveEventPlacesUseCase);
    }
}
