package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalBookingUseCase  implements BookingUseCase {
    private final BookingUseCase delegate;

    public TransactionalBookingUseCase(BookingUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBooking(BookingId bookingId) {
        return delegate.getBooking(bookingId);
    }

    @Override
    @Transactional
    public Booking createBooking(EventId eventId, UserId userId) {
        return delegate.createBooking(eventId, userId);
    }

    @Override
    @Transactional
    public Booking confirmBooking(BookingId bookingId) {
        return delegate.confirmBooking(bookingId);
    }

    @Override
    @Transactional
    public Booking cancelBooking(BookingId bookingId) {
        return delegate.cancelBooking(bookingId);
    }
}
