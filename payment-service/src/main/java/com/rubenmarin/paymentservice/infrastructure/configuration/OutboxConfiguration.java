package com.rubenmarin.paymentservice.infrastructure.configuration;

import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox.SpringDataOutboxRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfiguration {

    @Bean
    public OutboxRepository outboxRepository(SpringDataOutboxRepository springDataOutboxRepository){
        return new JpaOutboxRepositoryAdapter(springDataOutboxRepository);
    }
}
