package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class JpaBookingRepositoryAdapterTest {

    @Mock
    SpringDataBookingRepository springDataBookingRepository;

    @InjectMocks
    JpaBookingRepositoryAdapter repository;

    @Test
    void shouldNotTranslateUnrelatedConstraintViolation() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        Booking booking = new Booking(eventId, userId);

        // Simulamos una violación de otra restricción de PostgreSQL.
        ConstraintViolationException hibernateException =
                new ConstraintViolationException(
                        "NOT NULL constraint violated",
                        new SQLException("Constraint violation"),
                        "some_other_constraint"
                );

        DataIntegrityViolationException springException =
                new DataIntegrityViolationException(
                        "Data integrity violation",
                        hibernateException
                );

        // Simulamos que Spring Data JPA lanza la excepción.
        Mockito.when(springDataBookingRepository.saveAndFlush(Mockito.any()))
                .thenThrow(springException);

        // El adaptador NO debe convertir este error en BookingAlreadyExistsException.
        DataIntegrityViolationException thrown = Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(booking)
        );

        Assertions.assertSame(springException, thrown);
    }
}