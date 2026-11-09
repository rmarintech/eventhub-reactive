package com.rubenmarin.paymentservice.application.port.out;

import com.rubenmarin.paymentservice.domain.model.Payment;

public interface PaymentProcessor {

    boolean process(Payment payment);
}
