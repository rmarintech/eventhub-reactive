package com.rubenmarin.bookingservice.infrastructure.transaction;


import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;


// Sin @Transactional:
// Permite verificar el resultado después de que la transacción del caso de uso haya finalizado.
// Con Testcontainers, no necesitamos el rollback del test para limpiar la base de datos de desarrollo.

@SpringBootTest
@Testcontainers
class TransactionalBookingUseCaseIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @MockitoBean
    EventCapacityPort eventCapacityPort;

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
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(created.getEventId(), found.getEventId()),
                () -> Assertions.assertEquals(created.getUserId(), found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );

        Mockito.verify(eventCapacityPort).reservePlaces(eventId, 1);
        Mockito.verify(eventCapacityPort, Mockito.never()).releasePlaces(eventId, 1);
    }

    @Test
    void shouldPersistBookingConfirmation() {
        // PENDING → CONFIRMED
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        // 1. Crear reserva PENDING
        Booking created = bookingUseCase.createBooking(eventId, userId);

        // 2. Confirmarla mediante el caso de uso
        Booking confirmed = bookingUseCase.confirmBooking(created.getBookingId());

        // 3. Recuperarla directamente del repositorio
        Booking found = repository.findById(created.getBookingId()).orElseThrow();

//        - BookingUseCase ejecuta la transición PENDING → CONFIRMED.
//        - La actualización queda persistida en PostgreSQL.
//        - Se conserva el mismo BookingId.
//        - La instancia original created permanece en PENDING, demostrando la inmutabilidad del dominio.
//        Además, al no utilizar @Transactional en el test, comprobamos la persistencia
//        después de que el decorador haya finalizado la operación.

        // 4. Comprobar que se ha persistido la transición
        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, found.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, created.getStatus())
        );
    }

    @Test
    void shouldPersistBookingCancellation() {
        // PENDING → CANCELLED
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        // 1. Crear reserva PENDING
        Booking created = bookingUseCase.createBooking(eventId, userId);

        // 2. Cancelarla mediante el caso de uso
        Booking cancelled = bookingUseCase.cancelBooking(created.getBookingId());

        // 3. Recuperarla directamente del repositorio
        Booking found = repository.findById(created.getBookingId()).orElseThrow();

        // 4. Comprobar que se ha persistido la transición
        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, found.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, created.getStatus()),
                //las reservas canceladas dejan de estar activas.
                () -> Assertions.assertFalse(repository.existsActiveBooking(eventId, userId))
        );
    }

    @Test
    void shouldPersistBookingCancellationAfterConfirmation() {
        // PENDING → CONFIRMED → CANCELLED
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        // 1. Crear reserva
        Booking created = bookingUseCase.createBooking(eventId, userId);

        // 2. Confirmarla
        Booking confirmed = bookingUseCase.confirmBooking(created.getBookingId());

        // 3. Cancelarla
        Booking cancelled = bookingUseCase.cancelBooking(confirmed.getBookingId());

        // 4. Recuperarla desde PostgreSQL
        Booking found = repository.findById(created.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, found.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertFalse(repository.existsActiveBooking(eventId, userId))
        );
    }
}
