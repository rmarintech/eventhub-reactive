package com.rubenmarin.eventhub.event.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * Input data required to execute the Create Event use case.
 * <p>
 * This command belongs to the application layer.
 * It represents the intention to create a new Event.
 */
public record CreateEventCommand(
        String name,
        String description,
        LocalDateTime startDate,
        int capacity,
        BigDecimal price,
        Currency currency
) {

    public CreateEventCommand(
            String name,
            String description,
            LocalDateTime startDate,
            int capacity,
            BigDecimal price) {

        this(
                name,
                description,
                startDate,
                capacity,
                price,
                Currency.getInstance("EUR")
        );
    }
}

