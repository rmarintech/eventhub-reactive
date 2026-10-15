package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.domain.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataBookingRepository extends JpaRepository<BookingJpaEntity, UUID> {
//    Spring Data JPA Derived Queries
//    eventId coincide.
//    userId coincide.
//    status es diferente de CANCELLED.

    boolean existsByEventIdAndUserIdAndStatusNot(
            UUID eventId,
            UUID userId,
            BookingStatus status
    );
}
