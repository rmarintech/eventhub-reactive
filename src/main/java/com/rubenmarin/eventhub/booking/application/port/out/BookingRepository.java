package com.rubenmarin.eventhub.booking.application.port.out;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import reactor.core.publisher.Mono;

/**
 * Outbound port used by the application to persist Bookings.
 * <p>
 * The application defines what it needs from persistence
 * without depending on a specific database technology.
 */
public interface BookingRepository {

    Mono<Booking> create(Booking booking);

    Mono<Booking> update(Booking booking);

}