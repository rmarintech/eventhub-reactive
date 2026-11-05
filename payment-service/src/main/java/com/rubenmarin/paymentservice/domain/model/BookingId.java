package com.rubenmarin.paymentservice.domain.model;

import java.util.UUID;

public record BookingId(UUID bookingId) {

    public BookingId {
        if (bookingId == null) {
            throw new IllegalStateException("BookingId cannot be null");
        }
    }

    public static BookingId generateBookingId() {
        return new BookingId(UUID.randomUUID());
    }
}

