package com.rubenmarin.bookingservice.infrastructure.adapter.out.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.event.IntegrationEvent;
import com.rubenmarin.bookingservice.application.port.out.IntegrationEventPublisherPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;


public class IbmMqIntegrationEventAdapter implements IntegrationEventPublisherPort {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public IbmMqIntegrationEventAdapter(
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<Void> publish(IntegrationEvent event) {

        if (event instanceof BookingCreatedIntegrationEvent bookingCreated) {

            String json = objectMapper.writeValueAsString(event);
            jmsTemplate.convertAndSend("BOOKING.CREATED.Q", json);
            // Importante: JmsTemplate.convertAndSend() es síncrono. Si falla,
            // lanza una excepción; si termina correctamente,
            // puedes devolver CompletableFuture.completedFuture(null).

            return CompletableFuture.completedFuture(null);

        } else {
            throw new IllegalArgumentException("Unsupported integration event: " + event.getClass().getName());

        }

    }
}
