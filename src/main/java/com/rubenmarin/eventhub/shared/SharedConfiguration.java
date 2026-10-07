package com.rubenmarin.eventhub.shared;

import com.rubenmarin.eventhub.shared.application.port.out.DomainEventPublisher;
import com.rubenmarin.eventhub.shared.infrastructure.event.InMemoryDomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedConfiguration {

    @Bean
    public DomainEventPublisher domainEventPublisher(){

        return new InMemoryDomainEventPublisher();

    }
}



