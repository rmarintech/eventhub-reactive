package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.InboxEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class BookingCreatedKafkaConsumer {

    private final ObjectMapper mapper;
    private final InboxRepository inboxRepository;
    public BookingCreatedKafkaConsumer(
            ObjectMapper mapper,
            InboxRepository inboxRepository) {
        this.mapper = mapper;
        this.inboxRepository = inboxRepository;
    }

    private static final Logger _log = LoggerFactory.getLogger(BookingCreatedKafkaConsumer.class);
    @KafkaListener(
            topics = "booking-created",
            groupId = "eventhub-event-service"
    )
    public void consume(String payload){
        _log.info("Received event from kafka consumer - {}" , payload);

        BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent =
                mapper.readerFor(BookingCreatedIntegrationEvent.class).readValue(payload);

        _log.info("Received BookingCreated event: {}", bookingCreatedIntegrationEvent.integrationEventId());

        UUID eventId = bookingCreatedIntegrationEvent.integrationEventId();
        OffsetDateTime processedAt =  OffsetDateTime.now();
// no hace falta preguntar si existe, ya la bbdd  hará su trabajo no permitiendo inserir 2 registros con misma pk
//        Mono<Boolean> exists = inboxRepository.existsById(eventId);
//        if(!exists.block()){
//                InboxEvent inboxEvent = new InboxEvent(eventId, processedAt);
//                inboxRepository.save(inboxEvent).block();
//                _log.info("New Inboxed event: {}", bookingCreatedIntegrationEvent.integrationEventId());
//        }else{
//            _log.info("Ignoring event, It was already inboxed event: {}", bookingCreatedIntegrationEvent.integrationEventId());
//        }

        try {
            InboxEvent inboxEvent = new InboxEvent(eventId, processedAt);
            inboxRepository.save(inboxEvent).block();
            _log.info("New Inboxed event: {}", bookingCreatedIntegrationEvent.integrationEventId());
        }catch (DuplicateKeyException e){
            _log.info("Ignoring event, It was already inboxed event: {}", bookingCreatedIntegrationEvent.integrationEventId());
        }

    }
}
