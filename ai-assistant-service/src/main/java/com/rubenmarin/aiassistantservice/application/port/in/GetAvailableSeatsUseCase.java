package com.rubenmarin.aiassistantservice.application.port.in;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetAvailableSeatsUseCase {

    Mono<Integer> getAvailableSeats(UUID eventId);
}
