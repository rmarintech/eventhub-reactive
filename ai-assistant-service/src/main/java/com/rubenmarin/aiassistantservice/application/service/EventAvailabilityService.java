package com.rubenmarin.aiassistantservice.application.service;

import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.application.port.out.EventAvailabilityPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class EventAvailabilityService implements GetAvailableSeatsUseCase {

    private final EventAvailabilityPort eventAvailabilityPort;

    public EventAvailabilityService(EventAvailabilityPort eventAvailabilityPort) {
        this.eventAvailabilityPort = eventAvailabilityPort;
    }

    @Override
    public Mono<Integer> getAvailableSeats(UUID eventId) {
        return eventAvailabilityPort.getAvailableSeats(eventId);
    }
}
