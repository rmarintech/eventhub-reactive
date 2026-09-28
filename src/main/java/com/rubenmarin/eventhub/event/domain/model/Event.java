package com.rubenmarin.eventhub.event.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate Root for the Event aggregate.
 *
 * Event protects the business invariants related to
 * the lifecycle and capacity of an event.
 */
public class Event {

    private final EventId id;
    private final EventName name;
    private final String description;
    private final LocalDateTime startDate;
    private Capacity capacity;
    private final Money price;
    private EventStatus status;

    private Event(
            EventId id,
            EventName name,
            String description,
            LocalDateTime startDate,
            Capacity capacity,
            Money price,
            EventStatus status
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.startDate = Objects.requireNonNull(startDate);
        this.capacity = Objects.requireNonNull(capacity);
        this.price = Objects.requireNonNull(price);
        this.status = Objects.requireNonNull(status);
    }

    public static Event create(
            EventName name,
            String description,
            LocalDateTime startDate,
            Capacity capacity,
            Money price
    ) {
        return new Event(
                EventId.generate(),
                name,
                description,
                startDate,
                capacity,
                price,
                EventStatus.DRAFT
        );
    }

    public EventId id() {
        return id;
    }

    public EventName name() {
        return name;
    }

    public String description() {
        return description;
    }

    public LocalDateTime startDate() {
        return startDate;
    }

    public Capacity capacity() {
        return capacity;
    }

    public Money price() {
        return price;
    }

    public EventStatus status() {
        return status;
    }

    public void publish() {

        if (status != EventStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft events can be published"
            );
        }

        status = EventStatus.PUBLISHED;
    }

    public void cancel() {

        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Event is already cancelled"
            );
        }

        status = EventStatus.CANCELLED;
    }

    public void reservePlaces(int places) {

        if (status != EventStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Places can only be reserved for published events"
            );
        }

        capacity = capacity.reserve(places);
    }

    public void releasePlaces(int places) {

        capacity = capacity.release(places);
    }
}