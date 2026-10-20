package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.model.BrokerName;
import com.rubenmarin.bookingservice.application.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    List<OutboxJpaEntity> findByStatusAndBrokerName(OutboxEventStatus status, BrokerName brokerName);

}
