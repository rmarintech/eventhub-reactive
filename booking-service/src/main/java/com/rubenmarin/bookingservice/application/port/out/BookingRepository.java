package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

import java.util.Optional;

public interface BookingRepository {

    Optional<Booking> findById(BookingId id);

    Booking save(Booking booking);

    boolean existsActiveBooking(EventId eventId, UserId userId);
}
