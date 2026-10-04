package com.rubenmarin.eventhub.booking.application.port.in;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import reactor.core.publisher.Mono;

/**
 * Inbound port for the Create Booking use case.
 *
 * It defines what the application allows an external actor
 * to request without exposing any infrastructure technology.
 */
public interface CreateBookingUseCase {

    Mono<Booking> createBooking(CreateBookingCommand command);
}