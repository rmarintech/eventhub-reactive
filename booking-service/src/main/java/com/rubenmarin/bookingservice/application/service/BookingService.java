package com.rubenmarin.bookingservice.application.service;

import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.exception.BookingNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventNotAvailableException;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

public class BookingService implements BookingUseCase {

    private final BookingRepository repository;
    private final EventAvailabilityPort eventAvailabilityPort;

    public BookingService(BookingRepository repository, EventAvailabilityPort eventAvailabilityPort) {

        this.repository = repository;
        this.eventAvailabilityPort = eventAvailabilityPort;

    }

    @Override
    public Booking getBooking(BookingId bookingId) {
        return repository.findById(bookingId)
                .orElseThrow(() ->
                        new BookingNotFoundException(bookingId)
                );
    }

    @Override
    public Booking createBooking(EventId eventId, UserId userId) {

        if (repository.existsActiveBooking(eventId, userId)) {
            throw new BookingAlreadyExistsException(eventId, userId);
        }

        if (!eventAvailabilityPort.isAvailable(eventId)){
            throw new EventNotAvailableException(eventId);
        }

        Booking booking = new Booking(eventId, userId);
        return repository.save(booking);
    }

    @Override
    public Booking confirmBooking(BookingId bookingId) {
        Booking booking = this.getBooking(bookingId);

        Booking confirmed = booking.confirm();

        return repository.save(confirmed);
    }

    @Override
    public Booking cancelBooking(BookingId bookingId) {
        Booking booking = this.getBooking(bookingId);

        Booking cancelled = booking.cancel();

        return repository.save(cancelled);
    }


}