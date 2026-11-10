package com.rubenmarin.paymentservice.infrastructure.configuration;

import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.port.out.*;
import com.rubenmarin.paymentservice.application.service.CreatePaymentService;
import com.rubenmarin.paymentservice.application.service.ProcessPaymentRequestService;
import com.rubenmarin.paymentservice.infrastructure.adapter.in.TransactionalProcessPaymentRequestDecorator;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment.JpaPaymentRepositoryAdapter;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment.SpringDataPaymentRepository;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.processor.SimulatedPaymentProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PaymentConfiguration {

    @Bean
    public CreatePaymentUseCase createPaymentService(PaymentRepository paymentRepository) {
        return new CreatePaymentService(paymentRepository);
    }

    @Bean
    public PaymentRepository paymentRepository(SpringDataPaymentRepository springDataPaymentRepository) {
        return new JpaPaymentRepositoryAdapter(springDataPaymentRepository);
    }

    @Bean
    public PaymentProcessor paymentProcessor() {
        return new SimulatedPaymentProcessor();
    }

    @Bean
    public ProcessPaymentRequestService processPaymentRequestService(
            InboxRepository inboxRepository,
            PaymentRepository paymentRepository,
            CreatePaymentUseCase createPaymentUseCase,
            OutboxRepository outboxRepository,
            PaymentProcessor paymentProcessor,
            IntegrationEventSerializer integrationEventSerializer
    ) {

        return new ProcessPaymentRequestService(
                inboxRepository,
                paymentRepository,
                createPaymentUseCase,
                outboxRepository,
                paymentProcessor,
                integrationEventSerializer
        );
    }

    @Bean
    @Primary
    public ProcessPaymentRequestUseCase transactionalProcessPaymentRequestUseCase(
            ProcessPaymentRequestService delegate
    ) {
        return new TransactionalProcessPaymentRequestDecorator(delegate);
    }


}
