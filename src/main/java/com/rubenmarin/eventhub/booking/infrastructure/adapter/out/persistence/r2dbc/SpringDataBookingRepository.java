package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence.r2dbc;

import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface SpringDataBookingRepository extends ReactiveCrudRepository<BookingEntity, UUID> {

    Flux<BookingEntity> findByEventId(UUID eventId);
}
