package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Primary
public class JpaBookingRepositoryAdapter implements BookingRepository {

    private final SpringDataBookingRepository springDataBookingRepository;

    public  JpaBookingRepositoryAdapter(SpringDataBookingRepository springDataBookingRepository){
        this.springDataBookingRepository = springDataBookingRepository;
    }


    @Override
    public Optional<Booking> findById(BookingId id) {
        return springDataBookingRepository.findById(id.bookingId()).map(bookingJpaEntity ->  bookingJpaEntity.toDomain());
    }

    @Override
    public Booking save(Booking booking) {
        return  springDataBookingRepository.save(BookingJpaEntity.fromDomain(booking)).toDomain();

    }

    @Override
    public boolean existsActiveBooking(EventId eventId, UserId userId) {
       return springDataBookingRepository.existsByEventIdAndUserIdAndStatusNot(eventId.eventId(),userId.userId(), BookingStatus.CANCELLED);
    }
}
