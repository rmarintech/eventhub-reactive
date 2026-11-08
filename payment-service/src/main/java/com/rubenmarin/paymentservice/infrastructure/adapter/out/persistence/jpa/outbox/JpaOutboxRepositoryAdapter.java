package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;

import java.util.List;

public class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataOutboxRepository springDataOutboxRepository;

    public JpaOutboxRepositoryAdapter(SpringDataOutboxRepository springDataOutboxRepository) {
        this.springDataOutboxRepository =  springDataOutboxRepository;
    }


    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxJpaEntity outboxJpaEntity=   springDataOutboxRepository.save(OutboxMapper.toEntity(event));

        return OutboxMapper.toDomain(outboxJpaEntity);
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        List<OutboxJpaEntity>  entityList = springDataOutboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        return entityList.stream().map(outboxJpaEntity -> OutboxMapper.toDomain(outboxJpaEntity)).toList();
    }
}
