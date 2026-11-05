package com.rubenmarin.paymentservice.application.port.out;

import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentId;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByPaymentRequestId(PaymentRequestId paymentRequestId);
}
