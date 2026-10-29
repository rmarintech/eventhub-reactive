package com.rubenmarin.eventhub.event.infrastructure.recovery;

import com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka.KafkaHeadersCodec;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class DltRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryService.class);
    private final SpringDataEventRecoveryRepository recoveryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaHeadersCodec kafkaHeadersCodec;

    public DltRecoveryService(
            SpringDataEventRecoveryRepository recoveryRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaHeadersCodec kafkaHeadersCodec
    ) {
        this.recoveryRepository = recoveryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaHeadersCodec = kafkaHeadersCodec;
    }

    public Mono<Void> reprocess() {

        return recoveryRepository
                .findAllByStatus(RecoveryStatus.PENDING)
                .concatMap(eventRecovery -> {

                    UUID attemptId = UUID.randomUUID();
                    eventRecovery.markAsProcessing(attemptId);

                    return recoveryRepository.save(eventRecovery)
                            .flatMap(eventRecoveryEntity -> {

                                return Mono.defer(() -> {

                                            Headers headers = kafkaHeadersCodec.deserialize(
                                                    eventRecoveryEntity.getHeaders()
                                            );

                                            ProducerRecord<String, String> record =
                                                    new ProducerRecord<>(
                                                            "booking-created",
                                                            null,
                                                            eventRecoveryEntity.getMessageKey(),
                                                            eventRecoveryEntity.getPayload(),
                                                            headers
                                                    );

                                            return Mono.fromFuture(kafkaTemplate.send(record));
                                        })
                                        .onErrorResume(ex -> {
                                            log.error("Error publishing to Kafka: {}", ex.getMessage(), ex);
                                            eventRecoveryEntity.markAsPendingFromProcessing();
                                            return recoveryRepository.save(eventRecoveryEntity).then(Mono.empty());
                                        })
                                        .flatMap(result -> {
                                            eventRecoveryEntity.markAsRepublished(attemptId);
                                            return recoveryRepository.save(eventRecoveryEntity);
                                        });
                            });


                })
                .then();

    }
}
