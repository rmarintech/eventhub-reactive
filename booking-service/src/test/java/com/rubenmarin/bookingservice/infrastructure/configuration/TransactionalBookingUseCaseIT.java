package com.rubenmarin.bookingservice.infrastructure.configuration;


import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

@SpringBootTest
@Testcontainers
class TransactionalBookingUseCaseIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    BookingUseCase bookingUseCase;

    @Autowired
    BookingRepository repository;

    @Test
    void shouldCommitBookingCreation() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking created = bookingUseCase.createBooking(eventId, userId);

        //comprobamos resultado sin pasar por lógica de negocio
        Booking found = repository.findById(created.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(found.getBookingId(), created.getBookingId()),
                () -> Assertions.assertEquals(found.getEventId(), created.getEventId()),
                () -> Assertions.assertEquals(found.getUserId(), created.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );


    }
}
