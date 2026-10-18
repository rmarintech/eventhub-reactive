package com.rubenmarin.bookingservice.infrastructure.transaction;


import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEventStatus;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import com.rubenmarin.bookingservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
    BookingRepository bookingRepository;

   // @Autowired
    @MockitoSpyBean //mantiene la implementación real del repositorio, pero permite simular una excepción con Mockito.
    OutboxRepository outboxRepository;

    @Autowired
    ObjectMapper objectMapper ;

    @Test
    void shouldCommitBookingCreation() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking created = bookingUseCase.createBooking(eventId, userId);

        //comprobamos resultado sin pasar por lógica de negocio
        Booking found = bookingRepository.findById(created.getBookingId()).orElseThrow();

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
    void shouldCommitBookingCreationAndOutboxEvent() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking created = bookingUseCase.createBooking(eventId, userId);

        //comprobamos resultado sin pasar por lógica de negocio
        Booking found = bookingRepository.findById(created.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(created.getEventId(), found.getEventId()),
                () -> Assertions.assertEquals(created.getUserId(), found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );

        Mockito.verify(eventCapacityPort).reservePlaces(eventId, 1);
        Mockito.verify(eventCapacityPort, Mockito.never()).releasePlaces(eventId, 1);

        UUID createdBookingId = created.getBookingId().bookingId();

        OutboxEvent  outbox= outboxRepository
                .findByStatusPending()
                .stream()
                .filter(
                        outboxEvent ->
                                outboxEvent.aggregateId().equals(createdBookingId))
                .findFirst().orElseThrow(
                        () -> new RuntimeException("Outbox event not found for booking Id: " + createdBookingId)
                );

        JsonNode json = objectMapper.readTree(outbox.payload());


        Assertions.assertAll(
                () -> Assertions.assertEquals( OutboxEventStatus.PENDING, outbox.status()),
                () -> Assertions.assertEquals("BookingCreated", outbox.eventType()),
                () -> Assertions.assertEquals("booking-created", outbox.topic()),

                () -> Assertions.assertEquals(createdBookingId.toString(), json.get("bookingId").asString()),
                () -> Assertions.assertEquals(eventId.eventId().toString(), json.get("eventId").asString()),
                () -> Assertions.assertEquals(userId.userId().toString(), json.get("userId").asString())
        );
    }

    @Test
    void shouldRollbackBookingWhenOutboxFails() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        // Simulamos un fallo al persistir el evento Outbox.
        Mockito.doThrow(new RuntimeException("Outbox persistence failed"))
                .when(outboxRepository)
                .createOutboxEvent(Mockito.any(OutboxEvent.class));

        // La creación de la reserva debe fallar.
        Assertions.assertThrows(
                RuntimeException.class,
                () -> bookingUseCase.createBooking(eventId, userId)
        );

        // La transacción debe revertir el INSERT del Booking.
        Assertions.assertFalse(                bookingRepository.existsActiveBooking(eventId, userId)        );

        // La capacidad remota debe quedar compensada.
        Mockito.verify(eventCapacityPort).reservePlaces(eventId, 1);
        Mockito.verify(eventCapacityPort).releasePlaces(eventId, 1);
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
        Booking found = bookingRepository.findById(created.getBookingId()).orElseThrow();

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
        Booking found = bookingRepository.findById(created.getBookingId()).orElseThrow();

        // 4. Comprobar que se ha persistido la transición
        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, found.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, created.getStatus()),
                //las reservas canceladas dejan de estar activas.
                () -> Assertions.assertFalse(bookingRepository.existsActiveBooking(eventId, userId))
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
        Booking found = bookingRepository.findById(created.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(created.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, found.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertFalse(bookingRepository.existsActiveBooking(eventId, userId))
        );
    }

}
