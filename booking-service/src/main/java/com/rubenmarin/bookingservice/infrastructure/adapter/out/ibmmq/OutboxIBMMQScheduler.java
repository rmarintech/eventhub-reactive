package com.rubenmarin.bookingservice.infrastructure.adapter.out.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.model.BrokerName;
import com.rubenmarin.bookingservice.application.port.out.IntegrationEventPublisherPort;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Component
public class OutboxIBMMQScheduler {

    private static final Logger _log = LoggerFactory.getLogger(OutboxIBMMQScheduler.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IntegrationEventPublisherPort integrationEventPublisher;

    public OutboxIBMMQScheduler(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            @Qualifier("ibmMqIntegrationEventAdapter") IntegrationEventPublisherPort integrationEventPublisher
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    //fixedDelay = 5 segundos desde que termina una ejecución antes de iniciar la siguiente.
    //fixedRate = cada 5 segundos desde el inicio de la anterior.
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        outboxRepository.findByStatusPendingAndBrokerName(BrokerName.IBMMQ).forEach(outbox -> {


            BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent =
                    objectMapper.readValue(
                            outbox.payload(),
                            BookingCreatedIntegrationEvent.class
                    );

            //Importante: thenRun() no se ejecutará si falla.
            // En ese caso, el evento seguirá PENDING.
            CompletableFuture<Void> future = integrationEventPublisher.publish(bookingCreatedIntegrationEvent)
                    .thenRun(() ->
                    {
                        _log.debug("Published outbox: {}", outbox.id());

                        outboxRepository.markAsPublished(outbox.id());
                    })
                    .exceptionally(throwable -> {
                        _log.error("Outbox publication failed: {}", outbox.id(), throwable);
                        return null;
                    });

        });
    }
}
