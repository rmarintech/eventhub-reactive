package com.rubenmarin.bookingservice.application.port.in;

import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

public interface BookingUseCase {

    public Booking getBooking(BookingId id);

    Booking createBooking(EventId eventId, UserId userId);

    Booking confirmBooking(BookingId bookingId);

    Booking cancelBooking(BookingId bookingId);
}
