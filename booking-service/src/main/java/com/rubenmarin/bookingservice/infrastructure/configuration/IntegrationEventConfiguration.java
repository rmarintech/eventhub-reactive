package com.rubenmarin.bookingservice.infrastructure.configuration;


import com.rubenmarin.bookingservice.application.port.out.IntegrationEventPublisherPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.kafka.KafkaIntegrationEventAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class IntegrationEventConfiguration {

    @Bean
    public IntegrationEventPublisherPort integrationEventPublisherPort(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new KafkaIntegrationEventAdapter(
                kafkaTemplate,
                objectMapper
        );
    }


}
