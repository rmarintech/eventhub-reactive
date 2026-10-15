package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.domain.model.EventId;

public interface EventAvailabilityPort {
    boolean isAvailable(EventId eventId);
}
