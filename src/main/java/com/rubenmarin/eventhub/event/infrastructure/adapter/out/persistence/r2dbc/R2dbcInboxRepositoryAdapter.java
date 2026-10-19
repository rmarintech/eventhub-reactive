package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public class R2dbcInboxRepositoryAdapter implements InboxRepository {

    private final SpringDataInboxRepository    springDataInboxRepository;

    public R2dbcInboxRepositoryAdapter(SpringDataInboxRepository springDataInboxRepository) {

        this.springDataInboxRepository = springDataInboxRepository;
    }

    @Override
    public Mono<Boolean> existsById(UUID eventId) {
        return springDataInboxRepository.existsById(eventId);
    }

    @Override
    public Mono<InboxEvent> save(InboxEvent event) {

        Mono<InboxEvent> toRet =
                springDataInboxRepository
                        .save(toEntity(event,true))
                        .map(inboxEntity -> toDomain(inboxEntity));
        return toRet;
    }

    private InboxEntity toEntity(InboxEvent event, boolean isNew) {
        InboxEntity toRet = new InboxEntity(
                event.eventId(),
                event.processedAt(),
                isNew

        );
        return toRet;
    }

    private InboxEvent toDomain(InboxEntity inboxEntity) {
        InboxEvent  toRet = new InboxEvent(
                inboxEntity.eventId(),
                inboxEntity.processedAt()

        );
        return toRet;
    }




}
