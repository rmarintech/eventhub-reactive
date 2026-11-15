package com.rubenmarin.aiassistantservice.application.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventAvailabilityPort {

    Mono<Integer> getAvailableSeats(UUID eventId);
}
