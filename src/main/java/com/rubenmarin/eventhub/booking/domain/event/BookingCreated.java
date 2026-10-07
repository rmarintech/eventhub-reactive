package com.rubenmarin.eventhub.booking.domain.event;

import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;

public record BookingCreated (
        BookingId bookingId,
        CustomerId customerId,
        EventId eventId,
        int places) implements DomainEvent {
}
