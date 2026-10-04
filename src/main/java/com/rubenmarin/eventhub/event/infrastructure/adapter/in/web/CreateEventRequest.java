package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//{
//        "name":"Reactive Java Workshop",
//        "description":"Introduction to Project Reactor",
//        "startDate":"2026-11-15T10:00:00",
//        "capacity":20,
//        "price":49.99,
//        "currency":"EUR"
//}


public record CreateEventRequest(
        @NotBlank @Size(max = 100)
        String name,
        @NotBlank @Size(max = 100)
        String description,
        @NotNull
        LocalDateTime startDate,
        @Positive
        int capacity,
        @NotNull@PositiveOrZero
        BigDecimal price,
        @NotBlank
        String currency) {
}


