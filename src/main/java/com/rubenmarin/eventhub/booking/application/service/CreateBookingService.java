package com.rubenmarin.eventhub.booking.application.service;

import com.rubenmarin.eventhub.booking.application.mapper.BookingIntegrationEventMapper;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.application.port.out.DomainEventPublisher;
import com.rubenmarin.eventhub.shared.application.port.out.IntegrationEventPublisher;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ReserveEventPlacesUseCase reserveEventPlacesUseCase;
    private final DomainEventPublisher domainEventPublisher;

    public CreateBookingService(
            BookingRepository bookingRepository,
            ReserveEventPlacesUseCase reserveEventPlacesUseCase,
            DomainEventPublisher domainEventPublisher
            ) {

        this.bookingRepository = bookingRepository;
        this.reserveEventPlacesUseCase = reserveEventPlacesUseCase;
        this.domainEventPublisher = domainEventPublisher;
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

        Mono<Booking> createdBooking = reservedEvent.flatMap(
                event -> bookingRepository.create(booking));

        Mono<Booking> publishDomainEvent = createdBooking
                .flatMap(bookingCreated -> {

                    List<DomainEvent> domainEvents = bookingCreated.domainEvents();

                    Mono<Void> publishAll = Flux.fromIterable(domainEvents)
                            .flatMap(domainEvent -> {
                                return domainEventPublisher.publishDomainEvent(domainEvent);
                            })
                            .then();

                    return publishAll.then(Mono.just(bookingCreated));
                });

        return publishDomainEvent;

    }


}
