package com.rubenmarin.eventhub.booking.application.event;

import com.rubenmarin.eventhub.booking.domain.event.BookingCreated;
import com.rubenmarin.eventhub.booking.domain.model.BookingId;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.shared.application.event.DomainEventDispatcher;
import com.rubenmarin.eventhub.shared.application.handler.DomainEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

/*
BookingCreated
      ↓
dispatcher.dispatch(...)
      ↓
revisa eventType()
      ↓
BookingCreated.class coincide
      ↓
handler.handle(bookingCreated)
      ↓
Mono.empty()
      ↓
complete
 */

@ExtendWith(MockitoExtension.class)
class DomainEventDispatcherTest {

    @Mock
    DomainEventHandler<BookingCreated> domainEventHandler;


    @Test
    void shouldDispatchDomainEventToMatchingHandler() {

        BookingId bookingId = new BookingId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        EventId eventId = new EventId(UUID.randomUUID());
        int places = 3;

        BookingCreated bookingCreated = new BookingCreated(
                bookingId,
                customerId,
                eventId,
                places
        );

        Mockito.when(domainEventHandler.eventType())
                .thenReturn(BookingCreated.class);

        Mockito.when(domainEventHandler.handle(bookingCreated))
                .thenReturn(Mono.empty());

        DomainEventDispatcher dispatcher = new DomainEventDispatcher(List.of(domainEventHandler));

        // WHEN
        Mono<Void> result = dispatcher.dispatch(bookingCreated);

        // THEN
        StepVerifier.create(result)
                .verifyComplete();

        Mockito.verify(domainEventHandler).handle(bookingCreated);
    }
}
