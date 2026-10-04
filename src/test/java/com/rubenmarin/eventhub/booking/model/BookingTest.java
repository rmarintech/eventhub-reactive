package com.rubenmarin.eventhub.booking.model;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingStatus;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingTest {

    private Booking createBooking() {

        return Booking.generate(
                new CustomerId(UUID.randomUUID()),
                new EventId(UUID.randomUUID()),
                20
        );
    }


    @Test
    void shouldGenerateBookingAsConfirmed() {
        Booking booking = this.createBooking();

        Assertions.assertNotNull(booking.getBookingId());
        Assertions.assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }


    @Test
    void shouldCancelConfirmedBooking() {
        Booking booking = this.createBooking();

        booking.cancel();

        Assertions.assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void shouldNotCancelCancelledBooking() {
        Booking booking = this.createBooking();

        booking.cancel();

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> booking.cancel()
        );
    }


    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldNotGenerateBookingWithZeroPlaces(int places) {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Booking.generate(
                        new CustomerId(UUID.randomUUID()),
                        new EventId(UUID.randomUUID()),
                        places
                )
        );
    }
}
