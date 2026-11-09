package com.rubenmarin.paymentservice.infrastructure.configuration;

import com.rubenmarin.paymentservice.application.port.out.IntegrationEventSerializer;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.serializer.JacksonIntegrationEventSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SerializerConfiguration {

    @Bean
    public IntegrationEventSerializer integrationEventSerializer(ObjectMapper objectMapper) {
        return new JacksonIntegrationEventSerializer(objectMapper);
    }
}
