package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.*;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Primary
public class JpaBookingRepositoryAdapter implements BookingRepository {

    private static final String ACTIVE_BOOKING_CONSTRAINT = "uq_bookings_active_event_user";
    private final SpringDataBookingRepository springDataBookingRepository;

    public JpaBookingRepositoryAdapter(SpringDataBookingRepository springDataBookingRepository) {
        this.springDataBookingRepository = springDataBookingRepository;
    }


    @Override
    public Optional<Booking> findById(BookingId id) {
        return springDataBookingRepository.findById(id.bookingId()).map(bookingJpaEntity -> bookingJpaEntity.toDomain());
    }

    @Override
    public Booking save(Booking booking) {

        try {
            //saveAndFlush para que Hibernate ejecute las sentencias SQL pendientes antes de devolver el resultado
            // de manera que evalue si se viola el indice uq_bookings_active_event_user y poder hacer un rollback antes del commit.

            BookingJpaEntity entity = BookingJpaEntity.fromDomain(booking);
            return springDataBookingRepository.saveAndFlush(entity).toDomain();

        } catch (DataIntegrityViolationException ex) {

            Throwable cause = ex;

            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && ACTIVE_BOOKING_CONSTRAINT.equals(constraint.getConstraintName())) {
                    throw new BookingAlreadyExistsException(
                            booking.getEventId(),
                            booking.getUserId()
                    );
                }
                cause = cause.getCause();
            }

            throw ex;
        }

    }

    @Override
    public boolean existsActiveBooking(EventId eventId, UserId userId) {
        return springDataBookingRepository.existsByEventIdAndUserIdAndStatusNot(eventId.eventId(), userId.userId(), BookingStatus.CANCELLED);
    }
}
