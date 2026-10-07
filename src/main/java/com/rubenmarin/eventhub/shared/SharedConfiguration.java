package com.rubenmarin.eventhub.shared;

import com.rubenmarin.eventhub.shared.application.event.DomainEventDispatcher;
import com.rubenmarin.eventhub.shared.application.event.IntegrationEvent;
import com.rubenmarin.eventhub.shared.application.handler.DomainEventHandler;
import com.rubenmarin.eventhub.shared.application.port.out.DomainEventPublisher;
import com.rubenmarin.eventhub.shared.application.port.out.IntegrationEventPublisher;
import com.rubenmarin.eventhub.shared.infrastructure.event.InMemoryDomainEventPublisher;
import com.rubenmarin.eventhub.shared.infrastructure.event.InMemoryIntegrationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SharedConfiguration {

    @Bean
    public DomainEventPublisher domainEventPublisher(DomainEventDispatcher domainEventDispatcher){

        return new InMemoryDomainEventPublisher(domainEventDispatcher);

    }

    @Bean
    public DomainEventDispatcher domainEventDispatcher (List<DomainEventHandler<?>> domainEventHandlers) {
        return new DomainEventDispatcher(domainEventHandlers);
    }

    @Bean
    public IntegrationEventPublisher integrationEventPublisher(){
        return new InMemoryIntegrationEventPublisher();
    }


}



