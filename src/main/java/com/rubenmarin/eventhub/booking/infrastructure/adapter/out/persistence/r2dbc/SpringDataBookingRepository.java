package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface SpringDataBookingRepository extends ReactiveCrudRepository<BookingEntity, UUID> {
}
