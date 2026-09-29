package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence;

import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class InMemoryEventRepository implements EventRepository {

    private final Map<EventId, Event> events = new HashMap<>();

    @Override
    public Event save(Event event) {
        events.put(event.id(), event);
        return event;
    }
}