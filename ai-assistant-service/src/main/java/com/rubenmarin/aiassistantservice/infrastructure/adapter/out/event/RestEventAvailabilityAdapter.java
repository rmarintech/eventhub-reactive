package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.event;

import com.rubenmarin.aiassistantservice.application.exception.EventNotFoundException;
import com.rubenmarin.aiassistantservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.aiassistantservice.application.port.out.EventAvailabilityPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class RestEventAvailabilityAdapter implements EventAvailabilityPort {

    private final WebClient webClient;

    public RestEventAvailabilityAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Integer> getAvailableSeats(UUID eventId) {
        // retrieve() does not execute the HTTP request immediately.
        // Execution occurs when the resulting reactive publisher is subscribed to.
        // retrieve() converts HTTP 4xx and 5xx responses into WebClientResponseException errors.
        return webClient.get()
                .uri("/events/{id}", eventId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.just(new EventNotFoundException(eventId))
                )
                .onStatus(
                        status -> status.value() == 503,
                        response -> Mono.just(new EventServiceUnavailableException())
                )

                .bodyToMono(EventAvailabilityResponse.class)
                .map(response -> response.availableCapacity())
                .onErrorMap(throwable -> {
                            if (throwable instanceof WebClientRequestException) {
                                return new EventServiceUnavailableException(throwable);
                            } else
                                return throwable;

                        }
                );

        // WebClientRequestException represents request-level failures, such as connection problems.
        // WebClientResponseException represents HTTP error responses received from a server.
        // EventNotFoundException is our application exception and should propagate unchanged.

        // No DTO
        // .bodyToMono(JsonNode.class)
        // .map(json -> json.get("availableCapacity").asInt());
    }
}
