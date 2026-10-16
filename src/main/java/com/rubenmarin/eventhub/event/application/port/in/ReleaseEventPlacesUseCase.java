package com.rubenmarin.eventhub.event.application.port.in;

import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Mono;

public interface ReleaseEventPlacesUseCase {

    Mono<Event> releaseEventPlaces(EventId id, int places);
}
