package com.rubenmarin.aiassistantservice.infrastructure.config;

import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.aiassistantservice.application.service.EventAvailabilityService;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.event.RestEventAvailabilityAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EventConfiguration {

    @Bean
    public GetAvailableSeatsUseCase getAvailableSeatsUseCase(EventAvailabilityPort eventAvailabilityPort) {
        return new EventAvailabilityService(eventAvailabilityPort);
    }

    @Bean
    public EventAvailabilityPort eventAvailabilityPort(
            @Qualifier("eventWebClient")WebClient webClient
    ) {
        return new RestEventAvailabilityAdapter(webClient);
    }

    @Bean
    public WebClient eventWebClient(
            WebClient.Builder builder,
            @Value("${eventhub.event-service.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
