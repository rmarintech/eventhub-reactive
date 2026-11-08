package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    List<OutboxJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
