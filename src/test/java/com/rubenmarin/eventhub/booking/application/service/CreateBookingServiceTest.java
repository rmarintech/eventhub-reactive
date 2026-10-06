package com.rubenmarin.eventhub.booking.application.service;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingStatus;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.application.service.ReserveEventPlacesService;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

public class CreateBookingServiceTest {

    InMemoryEventRepository eventRepository;

    InMemoryBookingRepository bookingRepository;
    ReserveEventPlacesService reserveEventPlacesUseCase;
    CreateBookingService createBookingService;

    @BeforeEach
    void setUp() {

        eventRepository = new InMemoryEventRepository();

        bookingRepository = new InMemoryBookingRepository();
        reserveEventPlacesUseCase = new ReserveEventPlacesService(eventRepository);
        createBookingService = new CreateBookingService(bookingRepository, reserveEventPlacesUseCase);
    }

    @Test
    void shouldNotCreateBookingWhenNotEnoughPlaces() {
        Event newEvent = Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                new Money(new BigDecimal("49.99"), Currency.getInstance("EUR"))

        );

        newEvent.publish();
        CustomerId customerId = CustomerId.generate();

        Mono<Event> savedEvent = eventRepository.create(newEvent);


        Mono<Booking> booking = savedEvent.flatMap(e -> {
            CreateBookingCommand command = new CreateBookingCommand(
                    customerId,
                    e.id(),
                    30);
            return createBookingService.createBooking(command);
        });

        StepVerifier.create(booking)
                .expectError(IllegalStateException.class)
                .verify();

        Assertions.assertNull(bookingRepository.savedBooking);

    }

    @Test
    void shouldCreateBooking() {
        Event newEvent = Event.create(
                new EventName("Reactive Java Workshop"),
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                Capacity.of(20),
                new Money(new BigDecimal("49.99"), Currency.getInstance("EUR"))

        );

        newEvent.publish();
        CustomerId customerId = CustomerId.generate();
        EventId eventId = newEvent.id();

        Mono<Event> savedEvent = eventRepository.create(newEvent);


        Mono<Booking> booking = savedEvent.flatMap(e -> {
            CreateBookingCommand command = new CreateBookingCommand(
                    customerId,
                    e.id(),
                    3);
            return createBookingService.createBooking(command);
        });

        StepVerifier.create(booking)
                .assertNext(bookingCreated -> {
                    Assertions.assertNotNull(bookingCreated);
                    Assertions.assertEquals(customerId, bookingCreated.getCustomerId());
                    Assertions.assertEquals(eventId, bookingCreated.getEventId());
                    Assertions.assertEquals(3, bookingCreated.getPlaces());
                    Assertions.assertEquals(BookingStatus.CONFIRMED, bookingCreated.getStatus());
                })
                .verifyComplete();
    }


    private static class InMemoryEventRepository implements EventRepository {

        private Event savedEvent;

        @Override
        public Mono<Event> create(Event event) {

            // return Mono.defer(() -> Mono.just(this.savedEvent = event));

            return Mono.fromSupplier(() -> {
                this.savedEvent = event;
                return event;
            });
        }

        @Override
        public Mono<Event> update(Event event) {

            // return Mono.defer(() -> Mono.just(this.savedEvent = event));

            return Mono.fromSupplier(() -> {
                this.savedEvent = event;
                return event;
            });
        }

        @Override
        public Mono<Event> findById(EventId id) {
            return Mono.defer(() -> {
                        if (savedEvent == null || !savedEvent.id().equals(id)) {
                            return Mono.empty();
                        } else {
                            return Mono.just(savedEvent);
                        }
                    }
            );
        }

        @Override
        public Flux<Event> findAll() {
            return Flux.defer(() -> Mono.justOrEmpty(savedEvent));
        }
    }

    private static class InMemoryBookingRepository implements BookingRepository {

        private Booking savedBooking;

        @Override
        public Mono<Booking> create(Booking booking) {
            return Mono.fromSupplier(() -> {
                this.savedBooking = booking;
                return booking;
            });
        }

        @Override
        public Mono<Booking> update(Booking booking) {
            return Mono.fromSupplier(() -> {
                this.savedBooking = booking;
                return booking;
            });
        }

        @Override
        public Flux<Booking> findByEventId(EventId eventId) {
            return null;
        }
    }

}
