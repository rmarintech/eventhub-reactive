package com.rubenmarin.bookingservice.application.event;


import java.util.UUID;

public record BookingCreatedIntegrationEvent(
        UUID bookingId,
        UUID eventId,
        UUID userId
)  implements IntegrationEvent {
}
