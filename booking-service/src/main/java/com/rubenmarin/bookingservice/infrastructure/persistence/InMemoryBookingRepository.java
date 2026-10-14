package com.rubenmarin.bookingservice.infrastructure.persistence;

import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingRepository implements BookingRepository {


    private final Map<BookingId, Booking> bookings = new ConcurrentHashMap<>();


    @Override
    public Optional<Booking> findById(BookingId id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getBookingId(), booking);
        return booking;
    }
}
