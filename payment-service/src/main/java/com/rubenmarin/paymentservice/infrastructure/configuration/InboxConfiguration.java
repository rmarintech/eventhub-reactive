package com.rubenmarin.paymentservice.infrastructure.configuration;

import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox.JpaInboxRepositoryAdapter;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox.SpringDataInboxRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxConfiguration {

    @Bean
    public InboxRepository inboxRepository(SpringDataInboxRepository springDataInboxRepository) {
        return new JpaInboxRepositoryAdapter(springDataInboxRepository);
    }
}
