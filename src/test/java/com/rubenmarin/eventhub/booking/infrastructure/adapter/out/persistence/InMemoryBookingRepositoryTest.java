package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence;


import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class InMemoryBookingRepositoryTest {

    @Test
    void shouldSaveBooking() {

        Booking booking = Booking.generate(
                new CustomerId(UUID.randomUUID()),
                new EventId(UUID.randomUUID()),
                3
        );

        InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();

        Mono<Booking> savedBooking = bookingRepository.save(booking);

        StepVerifier.create(savedBooking)
                .expectNext(booking)
                .verifyComplete();
    }

}