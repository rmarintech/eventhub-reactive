package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.domain.model.*;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "bookings")
public class BookingJpaEntity {

    @Id
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;


    //   JPA exige un constructor sin argumentos que sea public o protected.
    //   Elegimos protected porque:
    //            1. Permite que Hibernate construya la entidad mediante sus mecanismos de instanciación.
    //            2. Evita exponer ese constructor como parte de la API pública para cualquier clase.
    //            3. Nos permite ofrecer otro constructor public con todos los datos necesarios.
    protected  BookingJpaEntity(){}

    private  BookingJpaEntity(UUID bookingId,UUID eventId, UUID userId, BookingStatus status){
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.userId = userId;
        this.status = status;
    }

    public static BookingJpaEntity fromDomain(Booking booking) {
        return new BookingJpaEntity(
                booking.getBookingId().bookingId(),
                booking.getEventId().eventId(),
                booking.getUserId().userId(),
                booking.getStatus()
        );
    }

    public Booking toDomain() {
        return Booking.rehydrate(
                new BookingId(bookingId),
                new EventId(eventId),
                new UserId(userId),
                status
        );
    }
}
