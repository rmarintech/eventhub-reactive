package com.rubenmarin.eventhub.booking.application.service;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import reactor.core.publisher.Mono;

public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ReserveEventPlacesUseCase reserveEventPlacesUseCase;

    public CreateBookingService(
            BookingRepository bookingRepository,
            ReserveEventPlacesUseCase reserveEventPlacesUseCase) {

        this.bookingRepository = bookingRepository;
        this.reserveEventPlacesUseCase = reserveEventPlacesUseCase;
    }

    @Override
    public Mono<Booking> createBooking(CreateBookingCommand command) {


        Booking booking = Booking.generate(
                command.customerId(),
                command.eventId(),
                command.places());

        Mono<Event> reservedEvent =
                reserveEventPlacesUseCase.reserveEventPlaces(
                        command.eventId(),
                        command.places()
                );

        Mono<Booking> saved = reservedEvent.flatMap(
                event -> bookingRepository.save(booking));

        return saved;

    }


}
