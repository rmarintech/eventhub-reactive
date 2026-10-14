package com.rubenmarin.bookingservice.domain.model;


import java.util.Objects;
import java.util.UUID;

public class Booking {

    private final BookingId bookingId;
    private final EventId eventId;
    private final UserId userId;
    private final BookingStatus status;


    public Booking(
            EventId eventId,
            UserId userId

    ) {
        this(new BookingId(UUID.randomUUID()),
                 eventId,
                userId,
                BookingStatus.PENDING
        );
    }

    private Booking(
            BookingId bookingId,
            EventId eventId,
            UserId userId,
            BookingStatus status

    ) {
        this.bookingId = Objects.requireNonNull(bookingId, "Booking Id cannot be null");
        this.eventId = Objects.requireNonNull(eventId, "Event Id cannot be null");
        this.userId = Objects.requireNonNull(userId, "User Id cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public Booking confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending bookings can be confirmed"
            );
        }

        return new Booking(
                bookingId,
                eventId,
                userId,
                BookingStatus.CONFIRMED
        );
    }

    public Booking cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Booking already cancelled"
            );
        }
        return new Booking(
                bookingId,
                eventId,
                userId,
                BookingStatus.CANCELLED
        );
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public EventId getEventId() {
        return eventId;
    }

    public UserId getUserId() {
        return userId;
    }
}
