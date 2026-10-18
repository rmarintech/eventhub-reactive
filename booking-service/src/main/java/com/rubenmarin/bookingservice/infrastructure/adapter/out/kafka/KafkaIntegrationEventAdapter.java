package com.rubenmarin.bookingservice.infrastructure.adapter.out.kafka;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.event.IntegrationEvent;
import com.rubenmarin.bookingservice.application.port.out.IntegrationEventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;


public class KafkaIntegrationEventAdapter implements IntegrationEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaIntegrationEventAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<Void> publish(IntegrationEvent event) {

        if (event instanceof BookingCreatedIntegrationEvent bookingCreated) {

            String json = objectMapper.writeValueAsString(bookingCreated);

            CompletableFuture<SendResult<String, String>> result =
                    kafkaTemplate
                            .send(
                                    "booking-created",
                                    bookingCreated.bookingId().toString(),
                                    json
                            );

            // Kafka confirma el envío → thenApply() se ejecuta y devuelve null,
            //  completando correctamente el CompletableFuture<Void>.
            //  Kafka falla → thenApply() no se ejecuta y el futuro resultante termina con una excepción.

            return result.thenApply(res->null);

        } else {
            throw new IllegalArgumentException("Unsupported integration event: " + event.getClass().getName());

        }

    }
}
