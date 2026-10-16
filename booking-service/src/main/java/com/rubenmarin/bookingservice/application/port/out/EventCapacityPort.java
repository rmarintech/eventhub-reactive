package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.domain.model.EventId;

public interface EventCapacityPort {

    void reservePlaces(EventId eventId, int places);

    void releasePlaces(EventId eventId, int places);
}
