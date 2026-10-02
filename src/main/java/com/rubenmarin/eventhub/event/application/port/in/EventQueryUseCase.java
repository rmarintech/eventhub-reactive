package com.rubenmarin.eventhub.event.application.port.in;

import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventQueryUseCase {

    Flux<Event> findAll();

    Mono<Event> findById(EventId id);


}
