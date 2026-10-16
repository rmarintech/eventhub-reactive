package com.rubenmarin.eventhub.event.domain.model;

import com.rubenmarin.eventhub.event.domain.model.exception.ExcessiveCapacityException;
import com.rubenmarin.eventhub.event.domain.model.exception.InsufficientCapacityException;

/**
 * Represents the capacity of an Event.
 * <p>
 * Domain invariants:
 * <p>
 * total > 0
 * available >= 0
 * available <= total
 */
public record Capacity(
        int total,
        int available
) {

    public Capacity {

        if (total <= 0) {
            throw new IllegalArgumentException("Total capacity must be greater than zero");
        }

        if (available < 0) {
            throw new IllegalArgumentException("Available capacity cannot be negative");
        }

        if (available > total) {
            throw new IllegalArgumentException("Available capacity cannot exceed total capacity");
        }
    }

    /**
     * Creates a new capacity where all places are initially available.
     */
    public static Capacity of(int total) {
        return new Capacity(total, total);
    }

    public Capacity reserve(int places) {
        if (places <= 0) {
            throw new IllegalArgumentException("Places to reserve must be greater than zero");
        }

        if (places > available) {
            throw new InsufficientCapacityException("Not enough available capacity");
        }

        return new Capacity(total, available - places);
    }

    public Capacity release(int places) {
        if (places <= 0) {
            throw new IllegalArgumentException("Places to reserve must be greater than zero");
        }

        if (places + available > total) {
            throw new ExcessiveCapacityException("Cannot release more places than total capacity");
        }


        return new Capacity(total, available + places);
    }
}