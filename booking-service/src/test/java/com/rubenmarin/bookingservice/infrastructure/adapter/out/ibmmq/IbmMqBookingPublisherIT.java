package com.rubenmarin.bookingservice.infrastructure.adapter.out.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class IbmMqBookingPublisherIT {

    @Autowired
    private IbmMqBookingPublisher ibmMqBookingPublisher;

    @Test
    void shouldPublishtoQueue() {

        UUID integrationEventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BookingCreatedIntegrationEvent bookingCreatedEvent =
                new BookingCreatedIntegrationEvent(
                        integrationEventId,
                        bookingId,
                        eventId,
                        userId
                );

        ibmMqBookingPublisher.publish(bookingCreatedEvent);


        //Consultar cola en powershell
        //"DISPLAY QLOCAL('BOOKING.CREATED.Q')" | docker exec -i eventhub-ibmmq runmqsc QM1
        //Consultar profundidad de cola
        // "DISPLAY QLOCAL('BOOKING.CREATED.Q') CURDEPTH" | docker exec -i eventhub-ibmmq runmqsc QM1
    }
}
