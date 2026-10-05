package com.rubenmarin.eventhub.booking.domain.model;

import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.event.domain.model.EventStatus;

import java.util.Objects;

public class Booking {

    private final BookingId bookingId;
    private final CustomerId customerId;
    private final EventId eventId;
    private final int places;
    private BookingStatus status;


    private Booking(BookingId bookingId,
                    CustomerId customerId,
                    EventId eventId,
                    int places,
                    BookingStatus status) {

        this.bookingId = Objects.requireNonNull(bookingId);
        this.customerId = Objects.requireNonNull(customerId);
        this.eventId = Objects.requireNonNull(eventId);
        if (places <= 0) {
            throw new IllegalArgumentException("Places must be greater than zero");
        }
        this.places = places;
        this.status = Objects.requireNonNull(status);
    }


    public static Booking rehydrate(

            BookingId bookingId,
            CustomerId customerId,
            EventId eventId,
            int places,
            BookingStatus status) {

        return new Booking(
                bookingId,
                customerId,
                eventId,
                places,
                status);
    }

    public static Booking generate(
            CustomerId customerId,
            EventId eventId,
            int places) {
        return new Booking(
                BookingId.generate(),
                customerId,
                eventId,
                places,
                BookingStatus.CONFIRMED);
    }


    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Booking is already cancelled"
            );
        }
        status = BookingStatus.CANCELLED;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public EventId getEventId() {
        return eventId;
    }

    public int getPlaces() {
        return places;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
