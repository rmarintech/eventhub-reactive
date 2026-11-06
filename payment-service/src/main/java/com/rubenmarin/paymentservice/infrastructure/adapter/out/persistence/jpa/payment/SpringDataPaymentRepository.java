package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByPaymentRequestId(UUID paymentRequestId);

}