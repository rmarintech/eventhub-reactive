package com.rubenmarin.eventhub.event.application.port.out;

import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface InboxRepository {
    //    comprobar si un evento ya ha sido procesado.
    public Mono<Boolean> existsById(UUID eventId);

    //registrar el evento como procesado.
    public Mono<InboxEvent> save(InboxEvent event);
}
