package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntityId;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DltRecoveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryConsumer.class);
    private static final String DLT_TOPIC = "booking-created.DLT";
    private static final String DLT_GROUP = "eventhub-dlt-recovery";
    private final SpringDataEventRecoveryRepository recoveryRepository;
    private final KafkaHeadersCodec kafkaHeadersCodec;

    public DltRecoveryConsumer(
            SpringDataEventRecoveryRepository recoveryRepository,
            KafkaHeadersCodec kafkaHeadersCodec) {
        this.recoveryRepository = recoveryRepository;
        this.kafkaHeadersCodec = kafkaHeadersCodec;

    }

    @KafkaListener(
            topics = DLT_TOPIC,
            groupId = DLT_GROUP,
            containerFactory = "dltRecoveryKafkaListenerContainerFactory"
    )
    public Mono<Void> consume(ConsumerRecord<String, String> record) {
        log.info(
                "Received DLT event: topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset()
        );

        String kafkaTopic = record.topic();
        Integer kafkaPartition = record.partition();
        Long kafkaOffset = record.offset();
        String messageKey = record.key();

        String headers = kafkaHeadersCodec.serialize(record.headers());
        String payload = record.value();

        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        Mono<Void> toRet = recoveryRepository.save(eventRecoveryEntity)
                .doOnNext(saved -> {
                    log.info("Recovery event persisted: {}", saved.getEventRecoveryEntityId());
                })
                .doOnError(ex ->
                        log.error(
                                "DLT persistence failed: topic={}, offset={}",
                                record.topic(),
                                record.offset(),
                                ex
                        )
                )
                .then()
                .onErrorResume(DuplicateKeyException.class, ex -> {
                    log.warn("Ignoring EventRecoveryEntity, It was already in Recovery system: {}", eventRecoveryEntity.getEventRecoveryEntityId());
                    return Mono.empty();
                });

        return toRet;
    }
}
