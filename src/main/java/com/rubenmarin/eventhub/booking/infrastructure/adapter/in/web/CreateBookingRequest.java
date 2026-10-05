package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest (
        @NotBlank
        String customerId,
        @NotBlank
        String eventId,
        @Positive
        int places) {

}
