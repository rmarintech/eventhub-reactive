package com.rubenmarin.eventhub.event.infrastructure.config;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.application.service.CreateEventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfiguration {

    @Bean
    public CreateEventUseCase createEventUseCase(EventRepository eventRepository) {
        return new CreateEventService(eventRepository);
    }
}
