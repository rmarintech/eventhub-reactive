package com.rubenmarin.eventhub.booking.application.port.out.event;

import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.application.event.IntegrationEvent;

public record BookingCreatedIntegrationEvent(

        BookingId bookingId,
        CustomerId customerId,
        EventId eventId,
        int places

) implements IntegrationEvent {
}
