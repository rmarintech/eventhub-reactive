package com.rubenmarin.bookingservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BookingTest {


    @Test
    // comprueba que una reserva pendiente pasa a cancelada.
    public void shouldCancelPendingBooking() {


        BookingId bookingId = new BookingId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        BookingStatus status = BookingStatus.PENDING;

        Booking booking = new Booking(bookingId, eventId, userId, status);
        Booking cancelled = booking.cancel();

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.status());

        Assertions.assertEquals(booking.id(), cancelled.id());
        Assertions.assertEquals(booking.eventId(), cancelled.eventId());
        Assertions.assertEquals(booking.userId(), cancelled.userId());

        Assertions.assertEquals(BookingStatus.PENDING, booking.status());

    }

    @Test
    //comprueba que una reserva confirmada pasa a cancelada.
    public void shouldCancelConfirmedBooking() {

        BookingId bookingId = new BookingId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        BookingStatus status = BookingStatus.CONFIRMED;

        Booking booking = new Booking(bookingId, eventId, userId, status);
        Booking cancelled = booking.cancel();

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.status());

        Assertions.assertEquals(booking.id(), cancelled.id());
        Assertions.assertEquals(booking.eventId(), cancelled.eventId());
        Assertions.assertEquals(booking.userId(), cancelled.userId());

        Assertions.assertEquals(BookingStatus.CONFIRMED, booking.status());

    }

    @Test
    // comprueba que se lanza IllegalStateException.
    public void shouldRejectAlreadyCancelledBooking() {

        BookingId bookingId = new BookingId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        BookingStatus status = BookingStatus.CANCELLED;

        Booking booking = new Booking(bookingId, eventId, userId, status);

        Assertions.assertThrows(IllegalStateException.class, () -> booking.cancel());
        Assertions.assertEquals(BookingStatus.CANCELLED, booking.status());

    }
}
