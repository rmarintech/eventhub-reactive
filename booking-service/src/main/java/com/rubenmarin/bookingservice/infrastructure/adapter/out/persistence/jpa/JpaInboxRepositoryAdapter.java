package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.port.out.InboxRepository;
import com.rubenmarin.bookingservice.application.model.InboxEvent;
import com.rubenmarin.bookingservice.domain.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaInboxRepositoryAdapter implements InboxRepository {

    private final SpringDataInboxRepository springDataInboxRepository;
    private final EntityManager entityManager;

    public JpaInboxRepositoryAdapter(
            SpringDataInboxRepository springDataInboxRepository,
            EntityManager entityManager) {
        this.springDataInboxRepository = springDataInboxRepository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsById(UUID eventId) {
        return springDataInboxRepository.existsById(eventId);
    }

    @Override
    public InboxEvent save(InboxEvent event) {


        // save() no garantiza un INSERT
        // Como eventId es un UUID asignado previamente,
        // Spring Data JPA puede interpretar la entidad como existente
        // y utilizar merge() en lugar de persist().
        //
        // Eso podría provocar una actualización en vez de rechazar un duplicado.
        // Para nuestro Inbox queremos un INSERT, de manera que PostgreSQL
        //
        // rechace cualquier event_id repetido mediante la clave primaria.
        // InboxEntity inboxEntity = springDataInboxRepository.save(toEntity(event));

        InboxEntity inboxEntity = toEntity(event);
        entityManager.persist(inboxEntity);
        return toDomain(inboxEntity);
    }

    private InboxEntity toEntity(InboxEvent event) {
        InboxEntity toRet = new InboxEntity(
                event.eventId(),
                event.processedAt()
        );
        return toRet;
    }

    private InboxEvent toDomain(InboxEntity inboxEntity) {
        InboxEvent toRet = new InboxEvent(
                inboxEntity.getEventId(),
                inboxEntity.getProcessedAt()

        );
        return toRet;
    }


}
