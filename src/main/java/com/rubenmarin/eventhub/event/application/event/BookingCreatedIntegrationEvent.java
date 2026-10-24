package com.rubenmarin.eventhub.event.application.event;


import java.util.UUID;

public record BookingCreatedIntegrationEvent(
        UUID integrationEventId,
        UUID bookingId,
        UUID eventId,
        UUID userId
)  implements IntegrationEvent {
}
