package com.rubenmarin.eventhub.event.domain.model;

import java.util.UUID;

/**
 * Strongly typed identifier for an Event.
 *
 * This is a Domain Value Object.
 *
 * Using EventId instead of UUID directly prevents mixing
 * identifiers belonging to different domain concepts.
 */
public record EventId(UUID value) {

    public EventId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Event id cannot be null"
            );
        }
    }

    /**
     * Creates a new unique Event identifier.
     */
    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }
}