package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private CreateBookingUseCase createBookingUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {

        BookingController bookingController =
                new BookingController(createBookingUseCase);

        webTestClient = WebTestClient
                .bindToController(bookingController)
                .build();
    }

    @Test
    void shouldCreateBooking() {

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = Booking.generate(
                customerId,
                eventId,
                3
        );

        when(createBookingUseCase.createBooking(
                any(CreateBookingCommand.class)
        )).thenReturn(Mono.just(booking));

        CreateBookingRequest createBookingRequest  =
                new CreateBookingRequest(
                        customerId.value().toString(),
                        eventId.value().toString(),
                        3
                );

        webTestClient.post()
                .uri("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createBookingRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(
                        booking.bookingId().value().toString()
                )
                .jsonPath("$.customerId").isEqualTo(
                        customerId.value().toString()
                )
                .jsonPath("$.eventId").isEqualTo(
                        eventId.value().toString()
                )
                .jsonPath("$.places").isEqualTo(3)
                .jsonPath("$.status").isEqualTo("CONFIRMED");
    }
}