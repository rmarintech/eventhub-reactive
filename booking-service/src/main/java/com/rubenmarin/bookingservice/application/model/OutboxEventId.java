package com.rubenmarin.bookingservice.application.model;

import java.util.UUID;

public record OutboxEventId(
        UUID id
) {

}
