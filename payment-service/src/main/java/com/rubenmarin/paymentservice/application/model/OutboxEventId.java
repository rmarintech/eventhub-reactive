package com.rubenmarin.paymentservice.application.model;

import java.util.UUID;

public record OutboxEventId(
        UUID id
) {

}
