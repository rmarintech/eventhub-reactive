package com.rubenmarin.bookingservice.infrastructure.transaction;


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
class TransactionalBookingRollbackIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    BookingUseCase bookingUseCase;

    @Autowired
    BookingRepository repository;

    // Registra una implementación de BookingUseCase específica para este test,
    // sin modificar BookingService en producción.
    @TestConfiguration
    static class RollbackTestConfiguration {

        @Bean
        @Primary
        BookingUseCase failingBookingUseCase(BookingRepository repository) {
            BookingUseCase failingDelegate = new BookingUseCase() {

                @Override
                public Booking createBooking(EventId eventId, UserId userId) {
                    repository.save(new Booking(eventId, userId));
                    throw new IllegalStateException("Simulated failure");
                }

                @Override
                public Booking getBooking(BookingId bookingId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Booking confirmBooking(BookingId bookingId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Booking cancelBooking(BookingId bookingId) {
                    throw new UnsupportedOperationException();
                }
            };

            return new TransactionalBookingUseCase(failingDelegate);
        }
    }

    @Test
    void shouldRollbackWhenBookingCreationFails() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> bookingUseCase.createBooking(eventId, userId)
        );

        boolean exists = repository.existsActiveBooking(eventId, userId);

        Assertions.assertFalse(exists);
    }

}
