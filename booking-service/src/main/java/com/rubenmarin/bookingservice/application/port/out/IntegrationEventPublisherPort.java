package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.application.event.IntegrationEvent;

import java.util.concurrent.CompletableFuture;

public interface IntegrationEventPublisherPort {

    CompletableFuture<Void> publish(IntegrationEvent event);
}
