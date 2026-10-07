package com.rubenmarin.eventhub.booking.application.mapper;

import com.rubenmarin.eventhub.booking.application.port.out.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.eventhub.booking.domain.event.BookingCreated;

public class BookingIntegrationEventMapper {

    public BookingCreatedIntegrationEvent map( BookingCreated event) {

        return new BookingCreatedIntegrationEvent(
                event.bookingId(),
                event.customerId(),
                event.eventId(),
                event.places());

    }
}
