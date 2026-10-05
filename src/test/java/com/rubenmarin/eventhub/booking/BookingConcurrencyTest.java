package com.rubenmarin.eventhub.booking;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.application.port.in.PublishEventUseCase;
import com.rubenmarin.eventhub.event.domain.model.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private CreateEventUseCase createEventUseCase;

    @Autowired
    private PublishEventUseCase publishEventUseCase;

    @Autowired
    private CreateBookingUseCase createBookingUseCase;


    @Test
    void shouldFailConcurrentBooking() {

        CreateEventCommand createEventCommand = new CreateEventCommand(
                "Integration test event",
                "Integration test event",
                LocalDateTime.now().plusDays(30),
                3,
                new BigDecimal("10.00")
        );

        Mono<Event> givenEvent = createEventUseCase.createEvent(createEventCommand)
                .flatMap(event -> {
                    return publishEventUseCase.publishEvent(event.id());
                });

        Mono<Tuple2<Booking, Booking>> tuple = givenEvent.flatMap(event -> {
            CreateBookingCommand createBookingACommand =
                    new CreateBookingCommand(
                            new CustomerId(UUID.randomUUID()),
                            event.id(),
                            2);
            CreateBookingCommand createBookingBCommand =
                    new CreateBookingCommand(
                            new CustomerId(UUID.randomUUID()),
                            event.id(),
                            2);

            Mono<Booking> bookingA = createBookingUseCase.createBooking(createBookingACommand);
            Mono<Booking> bookingB = createBookingUseCase.createBooking(createBookingBCommand);

            return Mono.zip(bookingA, bookingB);
        });

        StepVerifier.create(tuple)
                .expectError(OptimisticLockingFailureException.class)
                .verify();
    }
}
