package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence;

import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentId;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;

import java.util.Optional;

public class JpaPaymentRepositoryAdapter implements PaymentRepository {
    private final SpringDataPaymentRepository springDataPaymentRepository;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository springDataPaymentRepository) {
        this.springDataPaymentRepository = springDataPaymentRepository;
    }

    @Override
    public Payment save(Payment payment) {

        PaymentJpaEntity savedEntity = springDataPaymentRepository.save(PaymentMapper.toEntity(payment));
        return PaymentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return springDataPaymentRepository
                .findById(paymentId.paymentId())
                .map(paymentJpaEntity -> PaymentMapper.toDomain(paymentJpaEntity));
    }

    @Override
   public Optional<Payment> findByPaymentRequestId(PaymentRequestId paymentRequestId){
        return springDataPaymentRepository
                .findByPaymentRequestId(paymentRequestId.paymentRequestId())
                .map(paymentJpaEntity -> PaymentMapper.toDomain(paymentJpaEntity));
    }
}
