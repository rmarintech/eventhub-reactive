package com.rubenmarin.eventhub.booking.application.handler;

import com.rubenmarin.eventhub.booking.application.mapper.BookingIntegrationEventMapper;
import com.rubenmarin.eventhub.booking.application.port.out.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.eventhub.booking.domain.event.BookingCreated;
import com.rubenmarin.eventhub.shared.application.handler.DomainEventHandler;
import com.rubenmarin.eventhub.shared.application.port.out.IntegrationEventPublisher;
import reactor.core.publisher.Mono;

/*
BookingCreated
      ↓
BookingCreatedHandler
      ↓
BookingIntegrationEventMapper
      ↓
BookingCreatedIntegrationEvent
      ↓
IntegrationEventPublisher
 */


public class BookingCreatedHandler
        implements DomainEventHandler<BookingCreated> {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final BookingIntegrationEventMapper bookingIntegrationEventMapper;

    public BookingCreatedHandler(
            IntegrationEventPublisher integrationEventPublisher,
            BookingIntegrationEventMapper bookingIntegrationEventMapper) {

        this.integrationEventPublisher = integrationEventPublisher;
        this.bookingIntegrationEventMapper = bookingIntegrationEventMapper;
    }

    @Override
    public Mono<Void> handle(BookingCreated event) {

        BookingCreatedIntegrationEvent integrationEvent =
                bookingIntegrationEventMapper.map(event);

        return integrationEventPublisher
                .publishIntegrationEvent(integrationEvent);
    }

    @Override
    public Class<BookingCreated> eventType() {
        return BookingCreated.class;
    }
}