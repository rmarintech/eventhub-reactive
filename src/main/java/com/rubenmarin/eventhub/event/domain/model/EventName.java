package com.rubenmarin.eventhub.event.domain.model;

/**
 * Represents the name of an Event.
 *
 * The Value Object guarantees that an EventName
 * can never exist in an invalid state.
 */
public record EventName(String value) {

    private static final int MAX_LENGTH = 100;

    public EventName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Event name cannot be blank"
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Event name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
}