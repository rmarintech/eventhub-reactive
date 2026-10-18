package com.rubenmarin.bookingservice.infrastructure.transaction;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Creamos TransactionalBookingUseCase como un decorador transaccional para
 * mantener nuestro BookingService independiente de Spring,
 * respetando la arquitectura hexagonal.
 * <p>
 * BookingController
 *      ↓
 * BookingUseCase
 *      ↓
 * CompensatingBookingUseCase
 *      ↓
 * TransactionalBookingUseCase
 *      │
 *      │ @Transactional
 *      ▼
 * BookingService
 *      ↓
 * BookingRepository
 *      ↓
 * PostgreSQL
 *
 */
public class TransactionalBookingUseCase implements BookingUseCase {
    private final BookingUseCase delegate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransactionalBookingUseCase(
            BookingUseCase delegate,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {

        this.delegate = delegate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBooking(BookingId bookingId) {
        return delegate.getBooking(bookingId);
    }

    @Override
    @Transactional
    public Booking createBooking(EventId eventId, UserId userId) {

        Booking booking = delegate.createBooking(eventId, userId);

        //Todo: create outbox_event

        BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent =
                new BookingCreatedIntegrationEvent(
                        booking.getBookingId().bookingId(),
                        booking.getEventId().eventId(),
                        booking.getUserId().userId()
                );


        String eventType = "BookingCreated";
        UUID aggregateId = bookingCreatedIntegrationEvent.bookingId();
        String topic = "booking-created";
        String payload = objectMapper.writeValueAsString(bookingCreatedIntegrationEvent);

        OutboxEvent outboxEvent = OutboxEvent.pending(
                eventType,
                aggregateId,
                topic,
                payload
        );


        outboxRepository.createOutboxEvent(outboxEvent);


        return booking;
    }

    @Override
    @Transactional
    public Booking confirmBooking(BookingId bookingId) {
        return delegate.confirmBooking(bookingId);
    }

    @Override
    @Transactional
    public Booking cancelBooking(BookingId bookingId) {
        return delegate.cancelBooking(bookingId);
    }
}
