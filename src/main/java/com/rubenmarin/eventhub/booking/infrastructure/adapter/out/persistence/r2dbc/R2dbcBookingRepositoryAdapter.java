package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence.r2dbc;

import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.BookingStatus;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.SpringDataEventRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Currency;

@Repository
public class R2dbcBookingRepositoryAdapter implements BookingRepository {

    private final SpringDataBookingRepository springDataBookingRepository;

    public R2dbcBookingRepositoryAdapter(SpringDataBookingRepository springDataBookingRepository) {

        this.springDataBookingRepository = springDataBookingRepository;
    }



    @Override
    public Mono<Booking> create(Booking booking) {

        Mono<Booking> toRet = springDataBookingRepository
                .save(toEntity(booking , true))
                .map(entity -> toDomain(entity));


        return toRet;
    }

    @Override
    public Mono<Booking> update(Booking booking) {

        Mono<Booking> toRet = springDataBookingRepository
                .save(toEntity(booking, false))
                .map(entity -> toDomain(entity));


        return toRet;
    }


    private BookingEntity toEntity(Booking booking, boolean isNew) {
        BookingEntity toRet = new BookingEntity(
                booking.getBookingId().value(),
                booking.getCustomerId().value(),
                booking.getEventId().value(),
                booking.getPlaces(),
                booking.getStatus().name(),
                isNew

        );
        return toRet;
    }

    private Booking toDomain(BookingEntity entity) {


        Booking toRet = Booking.rehydrate(
                new BookingId(entity.id()),
                new CustomerId(entity.customerId()),
                new EventId(entity.eventId()),
                entity.places(),
                BookingStatus.valueOf(entity.status())
        );
        return toRet;

    }
}
