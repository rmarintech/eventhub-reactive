package com.rubenmarin.bookingservice.infrastructure.transaction;


import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

public class CompensatingBookingUseCase implements BookingUseCase {

    private final BookingUseCase delegate;
    private final EventCapacityPort eventCapacityPort;

    public CompensatingBookingUseCase(
            BookingUseCase delegate,
            EventCapacityPort eventCapacityPort) {

        this.delegate = delegate;
        this.eventCapacityPort = eventCapacityPort;
    }

    @Override
    public Booking createBooking(EventId eventId, UserId userId) {

        eventCapacityPort.reservePlaces(eventId, 1);

        try {
            Booking booking = delegate.createBooking(eventId, userId);
            return booking;

        } catch (RuntimeException originalError) {

            try {
                eventCapacityPort.releasePlaces(eventId, 1);
            } catch (RuntimeException compensationError) {
                originalError.addSuppressed(compensationError);
            }

            throw originalError;
        }
    }

    @Override
    public Booking getBooking(BookingId bookingId) {
        return delegate.getBooking(bookingId);
    }

    @Override
    public Booking confirmBooking(BookingId bookingId) {
        return delegate.confirmBooking(bookingId);
    }

    @Override
    public Booking cancelBooking(BookingId bookingId) {
        return delegate.cancelBooking(bookingId);
    }
}

