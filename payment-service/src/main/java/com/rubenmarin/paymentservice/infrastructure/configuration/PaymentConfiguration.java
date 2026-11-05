package com.rubenmarin.paymentservice.infrastructure.configuration;

import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.application.service.CreatePaymentService;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.JpaPaymentRepositoryAdapter;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.SpringDataPaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

}
