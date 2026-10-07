package com.rubenmarin.eventhub.shared.application.port.out;

import com.rubenmarin.eventhub.shared.application.event.IntegrationEvent;
import reactor.core.publisher.Mono;

public interface IntegrationEventPublisher {
    Mono<Void> publishIntegrationEvent(IntegrationEvent event);
}
