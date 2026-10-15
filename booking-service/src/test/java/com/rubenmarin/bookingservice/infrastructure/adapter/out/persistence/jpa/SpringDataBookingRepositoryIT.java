package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;


import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingStatus;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SpringBootTest
// Oeraciones dentro de una transacción que Spring Test normalmente revierte al terminar.
@Transactional
class SpringDataBookingRepositoryIT {

    @Autowired
    SpringDataBookingRepository  repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void shouldSaveAndFindBooking() {
        UserId userId = new UserId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        BookingJpaEntity bookingJpa = BookingJpaEntity.fromDomain(booking);

        BookingJpaEntity savedJpa = repository.saveAndFlush(bookingJpa);
        entityManager.clear();

        BookingJpaEntity foundJpa = repository.findById(savedJpa.toDomain().getBookingId().bookingId()).orElseThrow();
        Booking found =foundJpa.toDomain();

        Assertions.assertAll(
                () -> Assertions.assertEquals(booking.getBookingId(), found.getBookingId()),
                () -> Assertions.assertEquals(eventId, found.getEventId()),
                () -> Assertions.assertEquals(userId, found.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, found.getStatus())
        );
    }
}
