package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEventId;
import com.rubenmarin.bookingservice.application.model.OutboxEventStatus;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataOutboxRepository  springDataOutboxRepository;

    public JpaOutboxRepositoryAdapter (SpringDataOutboxRepository  springDataOutboxRepository){
        this.springDataOutboxRepository = springDataOutboxRepository;
    }

    @Override
    public OutboxEvent createOutboxEvent(OutboxEvent event) {
        OutboxJpaEntity entity = OutboxJpaEntity.fromModel(event);
        return springDataOutboxRepository.saveAndFlush(entity).toModel();
    }

    @Override
    public List<OutboxEvent> findByStatusPending() {
        return springDataOutboxRepository
                .findByStatus(OutboxEventStatus.PENDING)
                .stream()
                .map(entity -> entity.toModel())
                .toList();
    }

    @Override
    //Operación ejecutada por scheduler, necesita su propia transacción
    // para recuperar y actualizar el registro.
    @Transactional
    public OutboxEvent markAsPublished(OutboxEventId outboxEventId) {

        OutboxJpaEntity entity=
                springDataOutboxRepository
                        .findById(outboxEventId.id()).orElseThrow(() -> new IllegalArgumentException(
                                "Outbox event not found: " + outboxEventId.id()
                        ));

        OutboxEvent published = entity.toModel().markPublished();
        entity.updateFromModel(published);

//        - Dirty checking: Hibernate detecta automáticamente los cambios en la entidad gestionada.
//        - Sin saveAndFlush(): al hacer commit, Hibernate ejecuta el UPDATE necesario.
//        - Sin merge(): evitamos crear otra entidad JPA y fusionar su estado.
//        Importante: esto funciona porque @Transactional mantiene la entidad gestionada por el EntityManager

        return entity.toModel();

    }
}
