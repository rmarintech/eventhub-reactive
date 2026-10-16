package com.rubenmarin.eventhub.event.infrastructure.config;

import com.rubenmarin.eventhub.event.application.port.in.*;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.application.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfiguration {

    @Bean
    public CreateEventUseCase createEventUseCase(EventRepository eventRepository) {
        return new CreateEventService(eventRepository);
    }

    @Bean
    public EventQueryUseCase eventQueryUseCase(EventRepository eventRepository) {
        return new EventQueryService(eventRepository);
    }

    @Bean
    public ReserveEventPlacesUseCase reserveEventPlacesUseCase(EventRepository eventRepository) {
        return new ReserveEventPlacesService(eventRepository);
    }

    @Bean
    public ReleaseEventPlacesUseCase releaseEventPlacesUseCase(EventRepository eventRepository) {
        return new ReleaseEventPlacesService(eventRepository);
    }

    @Bean
    public PublishEventUseCase publishUseCase(EventRepository eventRepository) {
        return new PublishEventService(eventRepository);
    }
}
