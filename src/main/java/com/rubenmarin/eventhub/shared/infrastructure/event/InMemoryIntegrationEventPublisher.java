package com.rubenmarin.eventhub.shared.infrastructure.event;

import com.rubenmarin.eventhub.shared.application.event.IntegrationEvent;
import com.rubenmarin.eventhub.shared.application.port.out.IntegrationEventPublisher;
import reactor.core.publisher.Mono;

public class InMemoryIntegrationEventPublisher implements IntegrationEventPublisher {
    @Override
    public Mono<Void> publishIntegrationEvent(IntegrationEvent event) {
        return Mono.empty();
    }
}
