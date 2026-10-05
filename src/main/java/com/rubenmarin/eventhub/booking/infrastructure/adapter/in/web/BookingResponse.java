package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.booking.domain.model.BookingStatus;

import java.util.UUID;


public record BookingResponse(
        UUID id,
        UUID customerId,
        UUID eventId,
        int places,
        String status
        ) {

}