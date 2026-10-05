package com.rubenmarin.eventhub.event.infrastructure.config;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.application.port.in.PublishEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.application.service.CreateEventService;
import com.rubenmarin.eventhub.event.application.service.EventQueryService;
import com.rubenmarin.eventhub.event.application.service.PublishEventService;
import com.rubenmarin.eventhub.event.application.service.ReserveEventPlacesService;
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
    public PublishEventUseCase publishUseCase(EventRepository eventRepository) {
        return new PublishEventService(eventRepository);
    }
}
