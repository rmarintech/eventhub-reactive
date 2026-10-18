package com.rubenmarin.bookingservice.infrastructure.adapter.out.kafka;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.port.out.IntegrationEventPublisherPort;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxScheduler {

    private static final Logger _log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IntegrationEventPublisherPort integrationEventPublisher;

    public OutboxScheduler(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            IntegrationEventPublisherPort integrationEventPublisher
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    //fixedDelay = 5 segundos desde que termina una ejecución antes de iniciar la siguiente.
    //fixedRate = cada 5 segundos desde el inicio de la anterior.
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        outboxRepository.findByStatusPending().forEach(outbox -> {


            BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent =
                    objectMapper.readValue(
                            outbox.payload(),
                            BookingCreatedIntegrationEvent.class
                    );

            //Importante: thenRun() no se ejecutará si Kafka falla.
            // En ese caso, el evento seguirá PENDING.
            integrationEventPublisher.publish(bookingCreatedIntegrationEvent)
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
