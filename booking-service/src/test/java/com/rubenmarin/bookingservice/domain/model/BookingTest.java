package com.rubenmarin.bookingservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BookingTest {

    @Test
    void shouldCreateBookingWithPendingStatus() {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        Assertions.assertAll(
                () -> Assertions.assertNotNull(booking.getBookingId()),
                () -> Assertions.assertEquals(eventId, booking.getEventId()),
                () -> Assertions.assertEquals(userId, booking.getUserId()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus())
        );
    }

    @Test
    void shouldConfirmPendingBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking confirmed = booking.confirm();

        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertEquals(booking.getBookingId(), confirmed.getBookingId()),
                () -> Assertions.assertEquals(booking.getEventId(), confirmed.getEventId()),
                () -> Assertions.assertEquals(booking.getUserId(), confirmed.getUserId()),
                () -> Assertions.assertNotSame(booking, confirmed)
        );
    }

    // comprueba que una reserva pendiente pasa a cancelada.
    @Test
    void shouldCancelPendingBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking cancelled = booking.cancel();

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        Assertions.assertEquals(booking.getBookingId(), cancelled.getBookingId());
        Assertions.assertEquals(booking.getEventId(), cancelled.getEventId());
        Assertions.assertEquals(booking.getUserId(), cancelled.getUserId());

        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());

        Assertions.assertNotSame(booking, cancelled);

    }

    //comprueba que una reserva confirmada pasa a cancelada.
    @Test
    void shouldCancelConfirmedBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );
        Booking confirmed = booking.confirm();
        Booking cancelled = confirmed.cancel();

        Assertions.assertEquals(booking.getBookingId(), cancelled.getBookingId());
        Assertions.assertEquals(booking.getEventId(), cancelled.getEventId());
        Assertions.assertEquals(booking.getUserId(), cancelled.getUserId());

        Assertions.assertNotSame(booking, confirmed);
        Assertions.assertNotSame(booking, cancelled);
        Assertions.assertNotSame(confirmed, cancelled);

        Assertions.assertAll(
                () -> Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus()),
                () -> Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus())
        );

    }

    // comprueba que se lanza IllegalStateException.
    @Test
    void shouldRejectAlreadyCancelledBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );
        Booking cancelled = booking.cancel();

        Assertions.assertThrows(IllegalStateException.class, () -> cancelled.cancel());

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());

        Assertions.assertNotSame(booking, cancelled);


    }

    // comprueba que se lanza IllegalStateException.
    @Test
    void shouldRejectConfirmationOfCancelledBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking cancelled = booking.cancel();

        Assertions.assertThrows(IllegalStateException.class, () -> cancelled.confirm());

        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());
        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        Assertions.assertNotSame(booking, cancelled);
    }

    // comprueba que se lanza IllegalStateException.
    @Test
    void shouldRejectConfirmationOfConfirmedBooking() {

        Booking booking = new Booking(
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking confirmed = booking.confirm();

        Assertions.assertThrows(IllegalStateException.class, () -> confirmed.confirm());

        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());
        Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());

        Assertions.assertNotSame(booking, confirmed);
    }
}
