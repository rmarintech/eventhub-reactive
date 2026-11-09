package com.rubenmarin.paymentservice.infrastructure.adapter.out.processor;

import com.rubenmarin.paymentservice.application.port.out.PaymentProcessor;
import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class SimulatedPaymentProcessor implements PaymentProcessor {
    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentProcessor.class);

    @Override
    public boolean process(Payment payment) {
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payments can be processed");
        }
        log.info("Simulated processing Payment, (succeeded if amount >100) ");

        return  0 < payment.getMoney().amount().compareTo(new BigDecimal("100"));
    }
}
