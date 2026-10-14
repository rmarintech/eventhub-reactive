package com.rubenmarin.bookingservice.application.exception;

import com.rubenmarin.bookingservice.domain.model.BookingId;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(BookingId bookingId) {
        super("Booking not found: " + bookingId.bookingId());
    }
}