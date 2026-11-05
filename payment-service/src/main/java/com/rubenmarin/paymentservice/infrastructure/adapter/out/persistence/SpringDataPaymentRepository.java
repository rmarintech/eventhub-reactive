package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence;

import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByPaymentRequestId(UUID paymentRequestId);

}