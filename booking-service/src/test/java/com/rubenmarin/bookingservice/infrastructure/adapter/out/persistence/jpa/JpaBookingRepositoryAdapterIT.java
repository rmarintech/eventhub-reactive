package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;


import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingStatus;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

@SpringBootTest
// Oeraciones dentro de una transacción que Spring Test normalmente revierte al terminar.
@Transactional
// 1/2 PostgreSQL 17 en un contenedor temporal
@Testcontainers
class JpaBookingRepositoryAdapterIT {

    // 2/2 PostgreSQL 17 en un contenedor temporal
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    BookingRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void shouldSaveAndFindBooking() {
        UserId userId = new UserId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);


//        1. save() registra la operación de persistencia.
//        2. flush() ejecuta las operaciones SQL pendientes. (ejecuta el INSERT) (no implica visibilidad para otras tx's)
//        3. clear() elimina las entidades gestionadas de la caché de primer nivel.
//        4 .findById() realiza una nueva lectura de PostgreSQL. (mediante SELECT)
//        5. PostgreSQL devuelve la fila porque la consulta ocurre dentro de la misma transacción.
//        6. Al terminar el test, Spring hace ROLLBACK y deshace la inserción.

        Booking saved = repository.save(booking);
        entityManager.flush();
        entityManager.clear();
        Booking found = repository.findById(saved.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(eventId, found.getEventId()),
                () -> Assertions.assertEquals(userId, found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );
    }

    @Test
    void shouldFindActiveBookingInPostgreSQL() {

        UserId userId = new UserId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

//       0.  repository.save() guarda una reserva PENDING.
//       1. flush() sincroniza los cambios con PostgreSQL.
//       2. clear() vacía el contexto de persistencia.
//       3. existsActiveBooking() ejecuta la consulta derivada de Spring Data JPA.
//       4. PostgreSQL encuentra una reserva activa y devuelve true.

        Booking saved = repository.save(booking);
        entityManager.flush();
        entityManager.clear();
        boolean found = repository.existsActiveBooking(eventId, userId);

        Assertions.assertTrue(found);

    }

    @Test
    void shouldNotFindCancelledBookingInPostgreSQL() {

        UserId userId = new UserId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        Booking cancelled = booking.cancel();

        repository.save(cancelled);
        entityManager.flush();
        entityManager.clear();
        boolean exists = repository.existsActiveBooking(eventId, userId);

        Assertions.assertFalse(exists);
    }

    @Test
    void shouldUpdateBookingStatusInPostgreSQL() {

        UserId userId = new UserId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        Booking saved = repository.save(booking);
        Booking confirmed = saved.confirm();
        Booking confirmedSaved = repository.save(confirmed);

        entityManager.flush();
        entityManager.clear();

        Booking found = repository.findById(confirmed.getBookingId()).orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(eventId, found.getEventId()),
                () -> Assertions.assertEquals(userId, found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, found.getStatus()),

                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus())
        );

    }

    // PENDING + PENDING	Rechazado
    @Test
    void shouldRejectDuplicateActiveBooking() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking1 = new Booking(eventId, userId);
        Booking booking2 = new Booking(eventId, userId);

        repository.save(booking1);

        // el repo ahora hace saveAndFlush por lo que ya no es necesario
        // Sin flush Hibernate puede aplazar la ejecución del INSERT
        //entityManager.flush();

        Assertions.assertThrows(
                                                        // PostgreSQL detecta la violación de uq_bookings_active_event_user.
                //DataIntegrityViolationException.class,// Spring la traduce a DataIntegrityViolationException.
                // ConstraintViolationException.class, // Hibernate lanza ConstraintViolationException.
                BookingAlreadyExistsException.class, // Nuestro adaptador la convierte en BookingAlreadyExistsException.
                () -> repository.save(booking2));


    }

    // CANCELLED + PENDING	Permitido
    @Test
    void shouldAllowNewBookingAfterCancellation() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        // Primera reserva: CANCELLED
        Booking booking1 = new Booking(eventId, userId);
        repository.save(booking1.cancel());

        entityManager.flush();
        entityManager.clear();

        // Segunda reserva: PENDING
        Booking booking2 = new Booking(eventId, userId);
        repository.save(booking2);

        entityManager.flush();
        entityManager.clear();

        Booking found = repository.findById(booking2.getBookingId())
                .orElseThrow();

        Assertions.assertAll(
                () -> Assertions.assertNotEquals(booking1.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(eventId, found.getEventId()),
                () -> Assertions.assertEquals(userId, found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );
    }

}
