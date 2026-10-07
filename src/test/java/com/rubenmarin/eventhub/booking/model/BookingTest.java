package com.rubenmarin.eventhub.booking.model;

import com.rubenmarin.eventhub.booking.domain.event.BookingCreated;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.BookingStatus;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
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

        Assertions.assertNotNull(booking.bookingId());
        Assertions.assertEquals(BookingStatus.CONFIRMED, booking.status());
    }


    @Test
    void shouldCancelConfirmedBooking() {
        Booking booking = this.createBooking();

        booking.cancel();

        Assertions.assertEquals(BookingStatus.CANCELLED, booking.status());
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


    @Test
    void shouldRegisterBookingCreatedEventWhenBookingIsGenerated() {
        // Arrange
        // crea CustomerId y EventId

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        int places = 3;
        // Act
        // Booking.generate(...)
        Booking booking = Booking.generate(
                customerId,
                eventId,
                places
        );

        // Assert
        // 1. hay exactamente un domain event
        // 2. es BookingCreated
        // 3. sus bookingId, customerId, eventId y places
        //    coinciden con los del Booking

        List<DomainEvent> domainEvents = booking.domainEvents();

        Assertions.assertEquals(1, domainEvents.size());
        Assertions.assertInstanceOf(BookingCreated.class, domainEvents.getFirst());

        BookingCreated eventCreated = (BookingCreated) domainEvents.getFirst();
        Assertions.assertEquals(booking.bookingId(), eventCreated.bookingId());
        Assertions.assertEquals(eventId, eventCreated.eventId());
        Assertions.assertEquals(customerId, eventCreated.customerId());
        Assertions.assertEquals(places, eventCreated.places());
    }

    @Test
    void shouldNotRegisterBookingCreatedEventWhenBookingIsRehydrated() {
        // Arrange
        BookingId bookingId = new BookingId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        int places = 3;
        BookingStatus status = BookingStatus.CONFIRMED;

        // Act
        // Booking.rehydrate(...)
        Booking booking = Booking.rehydrate(
                bookingId,
                customerId,
                eventId,
                places,
                status
        );

        // Assert
        // domainEvents está vacío
        Assertions.assertEquals(0, booking.domainEvents().size());
    }


}
