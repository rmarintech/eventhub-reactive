package com.rubenmarin.paymentservice.infrastructure.adapter.out.serializer;

import com.rubenmarin.paymentservice.application.model.IntegrationEvent;
import com.rubenmarin.paymentservice.application.port.out.IntegrationEventSerializer;
import tools.jackson.databind.ObjectMapper;

public class JacksonIntegrationEventSerializer implements IntegrationEventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonIntegrationEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(IntegrationEvent integrationEvent) {
        return objectMapper.writeValueAsString(integrationEvent);
    }
}
