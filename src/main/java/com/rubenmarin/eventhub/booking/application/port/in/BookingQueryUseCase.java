package com.rubenmarin.eventhub.booking.application.port.in;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Flux;

public interface BookingQueryUseCase {

    Flux<Booking> findByEventId(EventId eventId);
}
