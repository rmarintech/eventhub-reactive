package com.rubenmarin.paymentservice.infrastructure.adapter.out.serializer;

import com.rubenmarin.paymentservice.application.model.IntegrationEvent;
import com.rubenmarin.paymentservice.application.port.out.IntegrationEventSerializer;
import com.rubenmarin.paymentservice.domain.model.PaymentStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@SpringBootTest
class IntegrationEventSerializerTest {

    @Autowired
    IntegrationEventSerializer integrationEventSerializer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeIntegrationEventToJson() throws JsonProcessingException {

        UUID integrationEventId = UUID.randomUUID();
        UUID paymentRequestId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentStatus status = PaymentStatus.COMPLETED;

        IntegrationEvent integrationEvent = new IntegrationEvent(integrationEventId, paymentRequestId, bookingId, paymentId, status);

        String payload = integrationEventSerializer.serialize(integrationEvent);


        JsonNode json = objectMapper.readTree(payload);

        Assertions.assertNotNull(json);
        Assertions.assertEquals(integrationEventId.toString(), json.get("integrationEventId").asString());
        Assertions.assertEquals(paymentRequestId.toString(), json.get("paymentRequestId").asString());
        Assertions.assertEquals(bookingId.toString(), json.get("bookingId").asString());
        Assertions.assertEquals(paymentId.toString(), json.get("paymentId").asString());
        Assertions.assertEquals(status.toString(), json.get("status").asString());


    }
}
