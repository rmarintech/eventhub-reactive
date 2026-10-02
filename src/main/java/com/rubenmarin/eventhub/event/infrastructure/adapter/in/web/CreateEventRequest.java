package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web;


import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;

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
        String name,
        String description,
        LocalDateTime startDate,
        int capacity,
        BigDecimal price,
        String currency) {
}


