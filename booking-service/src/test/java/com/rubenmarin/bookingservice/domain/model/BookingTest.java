package com.rubenmarin.bookingservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BookingTest {

    @Test
    void shouldConfirmPendingBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
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

    @Test
    // comprueba que una reserva pendiente pasa a cancelada.
    public void shouldCancelPendingBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
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

    @Test
    //comprueba que una reserva confirmada pasa a cancelada.
    public void shouldCancelConfirmedBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
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

    @Test
    // comprueba que se lanza IllegalStateException.
    public void shouldRejectAlreadyCancelledBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );
        Booking cancelled = booking.cancel();

        Assertions.assertThrows(IllegalStateException.class, () -> cancelled.cancel());

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());

        Assertions.assertNotSame(booking, cancelled);


    }

    @Test
        // comprueba que se lanza IllegalStateException.
    void shouldRejectConfirmationOfCancelledBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
                new EventId(UUID.randomUUID()),
                new UserId(UUID.randomUUID())
        );

        Booking cancelled = booking.cancel();

        Assertions.assertThrows(IllegalStateException.class, () -> cancelled.confirm());

        Assertions.assertEquals(BookingStatus.PENDING, booking.getStatus());
        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        Assertions.assertNotSame(booking, cancelled);
    }

    @Test
        // comprueba que se lanza IllegalStateException.
    void shouldRejectConfirmationOfConfirmedBooking() {

        Booking booking = new Booking(
                new BookingId(UUID.randomUUID()),
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
