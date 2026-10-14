package com.rubenmarin.bookingservice.application.exception;

import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

public class BookingAlreadyExistsException extends RuntimeException {

    public BookingAlreadyExistsException(EventId eventId, UserId userId) {
        super("Booking already exists for user Id : " + userId.userId() + " and event Id : " + eventId.eventId());
    }
}