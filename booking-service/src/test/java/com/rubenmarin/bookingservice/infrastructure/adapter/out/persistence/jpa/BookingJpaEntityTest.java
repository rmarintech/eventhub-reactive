package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingStatus;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BookingJpaEntityTest {
    // Este test comprueba la conversión:
    // Booking → BookingJpaEntity → Booking
    @Test
    void shouldMapDomainToEntityAndBack() {

        // Arrange
        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        ).confirm();

        // Act
        BookingJpaEntity entity = BookingJpaEntity.fromDomain(booking);

        Booking restored = entity.toDomain();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(booking.getBookingId(), restored.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), restored.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), restored.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, restored.getStatus()),
                () -> Assertions.assertNotSame(booking, restored)
        );
    }
}
