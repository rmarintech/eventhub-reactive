package com.rubenmarin.eventhub.booking.domain.model;

import java.util.UUID;

/**
 * Strongly typed identifier for an Customer.
 *
 * This is a Domain Value Object.
 *
 * Using CustomerId instead of UUID directly prevents mixing
 * identifiers belonging to different domain concepts.
 */
public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Customer id cannot be null"
            );
        }
    }

    /**
     * Creates a new unique Customer identifier.
     */
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
}