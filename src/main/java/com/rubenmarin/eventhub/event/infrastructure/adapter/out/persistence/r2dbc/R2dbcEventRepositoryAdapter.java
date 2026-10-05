package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Currency;

@Repository
public class R2dbcEventRepositoryAdapter implements EventRepository {

    private final SpringDataEventRepository springDataEventRepository;

    public R2dbcEventRepositoryAdapter(SpringDataEventRepository springDataEventRepository) {

        this.springDataEventRepository = springDataEventRepository;
    }

    @Override
    public Mono<Event> create(Event event) {

        Mono<Event> toRet = springDataEventRepository
                .save(toEntity(event , true))
                .map(entity -> toDomain(entity));


        return toRet;
    }

    @Override
    public Mono<Event> update(Event event) {

        Mono<Event> toRet = springDataEventRepository
                .save(toEntity(event, false))
                .map(entity -> toDomain(entity));


        return toRet;
    }

    @Override
    public Mono<Event> findById(EventId id) {


        Mono<Event> toRet = springDataEventRepository
                .findById(id.value())
                .map(entity -> toDomain(entity));

        return toRet;
    }

    @Override
    public Flux<Event> findAll() {
        Flux<Event> toRet = springDataEventRepository
                .findAll()
                .map(entity -> toDomain(entity));

        return toRet;
    }

    private EventEntity toEntity(Event event, boolean isNew) {
        EventEntity toRet = new EventEntity(
                event.id().value(),
                event.name().value(),
                event.description(),
                event.startDate(),
                event.capacity().total(),
                event.capacity().available(),
                event.price().amount(),
                event.price().currency().getCurrencyCode(),
                event.status().name(),
                isNew

        );
        return toRet;
    }

    private Event toDomain(EventEntity entity) {


        Event toRet = Event.rehydrate(
                new EventId(entity.id()),
                new EventName(entity.name()),
                entity.description(),
                entity.startDate(),
                new Capacity(entity.capacityTotal(), entity.capacityAvailable()),
                new Money(entity.priceAmount(), Currency.getInstance(entity.priceCurrency())),
                EventStatus.valueOf(entity.status())
        );
        return toRet;

    }
}
