package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


public record EventResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime startDate,
        int capacity,
        BigDecimal price,
        String currency) {


}