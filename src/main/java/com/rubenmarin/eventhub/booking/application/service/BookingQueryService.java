package com.rubenmarin.eventhub.booking.application.service;

import com.rubenmarin.eventhub.booking.application.port.in.BookingQueryUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Flux;

public class BookingQueryService implements BookingQueryUseCase {

    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingRepository bookingRepository) {

        this.bookingRepository = bookingRepository;
    }

    @Override
    public Flux<Booking> findByEventId(EventId eventId) {
        Flux<Booking> toRet = bookingRepository.findByEventId(eventId);
        return toRet;
    }
}
