package com.rubenmarin.eventhub.booking.application.port.in;

import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * Input data required to execute the Create Booking use case.
 * <p>
 * This command belongs to the application layer.
 * It represents the intention to create a new Booking.
 */
public record CreateBookingCommand(
        CustomerId customerId,
        EventId eventId,
        int places) {
}

