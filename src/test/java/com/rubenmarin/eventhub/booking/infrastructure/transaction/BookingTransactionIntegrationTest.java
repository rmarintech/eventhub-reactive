package com.rubenmarin.eventhub.booking.infrastructure.transaction;

import com.rubenmarin.eventhub.booking.application.port.in.BookingQueryUseCase;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.application.port.in.PublishEventUseCase;
import com.rubenmarin.eventhub.event.domain.model.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@SpringBootTest
class BookingTransactionIntegrationTest {

    @Autowired
    BookingQueryUseCase bookingQueryUseCase;

    @Autowired
    EventQueryUseCase eventQueryUseCase;

    @Autowired
    CreateBookingUseCase createBookingUseCase;

    @Autowired
    PublishEventUseCase publishEventUseCase;

    @Autowired
    CreateEventUseCase createEventUseCase;

    @MockitoBean
    BookingRepository bookingRepository;

    @Test
    void shouldRollbackEventCapacityWhenBookingCreationFails() {
        CreateEventCommand createEventCommand =
                new CreateEventCommand(
                        "Reactive Java Workshop",
                        "Introduction to Project Reactor",
                        LocalDateTime.now().plusDays(30),
                        20,
                        new BigDecimal("49.99"),
                        Currency.getInstance("EUR")
                );

        Mono<Event> publishedEvent = createEventUseCase.createEvent(createEventCommand)
                .flatMap(event -> {
                    return publishEventUseCase.publishEvent(event.id());
                });

        Mockito.when(bookingRepository.create(Mockito.any())).thenReturn(Mono.error(new RuntimeException()));

        Mono<Integer> availableCapacityAfterFailure = publishedEvent.flatMap(event -> {
            CreateBookingCommand createBookingCommand =
                    new CreateBookingCommand(
                            new CustomerId(UUID.randomUUID()),
                            event.id(),
                            2);

            return createBookingUseCase.createBooking(createBookingCommand)
                    .onErrorResume(RuntimeException.class, error -> {
                        return Mono.empty();
                    })
                    .then(
                           eventQueryUseCase.findById(event.id())
                                   .map(found -> {
                                       return found.capacity().available();
                                   })
                    );

        });

        StepVerifier.create(availableCapacityAfterFailure)
                .assertNext(availableCapacity -> {
                    Assertions.assertEquals(20, availableCapacity);
                })
                .verifyComplete();

    }
}
