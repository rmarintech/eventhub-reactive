package com.rubenmarin.eventhub.booking.domain.model;

import com.rubenmarin.eventhub.booking.domain.event.BookingCreated;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Booking {

    private final BookingId bookingId;
    private final CustomerId customerId;
    private final EventId eventId;
    private final int places;
    private BookingStatus status;
    private final List<DomainEvent> domainEvents;

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
        this.domainEvents = new ArrayList<>();
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
                status
        );
    }

    public static Booking generate(
            CustomerId customerId,
            EventId eventId,
            int places) {


        BookingId bookingId = BookingId.generate();

        Booking booking = new Booking(
                bookingId,
                customerId,
                eventId,
                places,
                BookingStatus.CONFIRMED);

        booking.domainEvents.add(
                new BookingCreated(
                        bookingId,
                        customerId,
                        eventId,
                        places
                ));


        return booking;

    }


    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Booking is already cancelled"
            );
        }
        status = BookingStatus.CANCELLED;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public EventId eventId() {
        return eventId;
    }

    public int places() {
        return places;
    }

    public BookingStatus status() {
        return status;
    }

    public List<DomainEvent> domainEvents() {
        //Se puede leer pero no modificar
        return List.copyOf(domainEvents);
    }
}
