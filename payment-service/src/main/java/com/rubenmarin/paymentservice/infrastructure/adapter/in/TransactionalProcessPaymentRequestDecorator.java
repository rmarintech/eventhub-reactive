package com.rubenmarin.paymentservice.infrastructure.adapter.in;

import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.service.ProcessPaymentRequestService;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalProcessPaymentRequestDecorator implements ProcessPaymentRequestUseCase {

    private final ProcessPaymentRequestUseCase delegate;

    public TransactionalProcessPaymentRequestDecorator(ProcessPaymentRequestUseCase delegate) {
        this.delegate = delegate;
    }


    @Override
    @Transactional
    public void processPaymentRequest(ProcessPaymentRequestCommand processPaymentRequestCommand) {
        delegate.processPaymentRequest(processPaymentRequestCommand);

    }
}
