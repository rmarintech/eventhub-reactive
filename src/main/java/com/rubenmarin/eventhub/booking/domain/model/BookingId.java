package com.rubenmarin.eventhub.booking.domain.model;

import java.util.UUID;

/**
 * Strongly typed identifier for an Booking.
 *
 * This is a Domain Value Object.
 *
 * Using BookingId instead of UUID directly prevents mixing
 * identifiers belonging to different domain concepts.
 */
public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Booking id cannot be null"
            );
        }
    }

    /**
     * Creates a new unique Booking identifier.
     */
    public static BookingId generate() {
        return new BookingId(UUID.randomUUID());
    }
}