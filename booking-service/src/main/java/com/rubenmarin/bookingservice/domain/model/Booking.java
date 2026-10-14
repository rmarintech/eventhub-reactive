package com.rubenmarin.bookingservice.domain.model;

import java.util.UUID;

public record Booking(
        BookingId id,
        EventId eventId,
        UserId userId,
        BookingStatus status
) {

    public Booking confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending bookings can be confirmed"
            );
        }

        return new Booking(
                id,
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
                id,
                eventId,
                userId,
                BookingStatus.CANCELLED
        );
    }
}
