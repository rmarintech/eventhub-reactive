package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

//Ya no lo necesitamos @Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<BookingId, Booking> bookings = new HashMap<>();

    @Override
    public Mono<Booking> create(Booking booking) {

        // Ssí el put sucede antes de que nadie se subscriba
        // bookings.put(booking.id(), event);
        // return Mono.just(booking);
        // Mmejor con fromSupplier para que el put suceda al subscribirse:

        // Como es en memoria, no hace falta boundedElastic()
        return Mono.fromSupplier(() -> {
            bookings.put(booking.getBookingId(), booking);
            return booking;
        });

    }

    @Override
    public Mono<Booking> update(Booking booking) {

        // Ssí el put sucede antes de que nadie se subscriba
        // bookings.put(booking.id(), event);
        // return Mono.just(booking);
        // Mmejor con fromSupplier para que el put suceda al subscribirse:

        // Como es en memoria, no hace falta boundedElastic()
        return Mono.fromSupplier(() -> {
            bookings.put(booking.getBookingId(), booking);
            return booking;
        });
    }

}