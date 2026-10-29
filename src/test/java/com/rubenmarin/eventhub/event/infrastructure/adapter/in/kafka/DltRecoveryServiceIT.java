package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntityId;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import com.rubenmarin.eventhub.event.infrastructure.recovery.DltRecoveryService;
import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootTest
@Testcontainers
class DltRecoveryServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @MockitoSpyBean
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private DltRecoveryService dltRecoveryService;

    @Autowired
    private KafkaHeadersCodec kafkaHeadersCodec;

    @MockitoSpyBean(name = "kafkaTemplate")
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryServiceIT.class);
    private static final String DLT_TOPIC = "booking-created.DLT";

    @Test
    void shouldRepublishPendingEventSuccessfully() {

        // 1. Preparar evento PENDING
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = 25L;

        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        Headers originalHeaders = new RecordHeaders()
                .add(
                        "correlationId",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        // 2. Configurar consumidor Kafka independiente
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-recovery-it-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        try {
            consumer.subscribe(List.of("booking-created"));

            // 3. Persistir PENDING, republicar y verificar REPUBLISHED
            Mono<EventRecoveryEntity> operation =
                    dataEventRecoveryRepository
                            .save(eventRecoveryEntity)
                            .then(dltRecoveryService.reprocess())
                            .then(dataEventRecoveryRepository.findById(eventRecoveryEntityId));

            StepVerifier.create(operation)
                    .assertNext(eventRecovery -> {

                        Assertions.assertEquals(messageKey, eventRecovery.getMessageKey());
                        Assertions.assertEquals(payload, eventRecovery.getPayload());
                        Assertions.assertEquals(headers, eventRecovery.getHeaders());
                        Assertions.assertEquals(RecoveryStatus.REPUBLISHED, eventRecovery.getStatus());
                        Assertions.assertNotNull(eventRecovery.getAttemptId());
                        Assertions.assertNotNull(eventRecovery.getPendingAt());
                        Assertions.assertNotNull(eventRecovery.getProcessingAt());
                        Assertions.assertNotNull(eventRecovery.getRepublishedAt());

                    })
                    .verifyComplete();

            // 4. Verificar que el mensaje existe realmente en Kafka
            AtomicBoolean received = new AtomicBoolean(false);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(20))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                        for (ConsumerRecord<String, String> record : records) {
                            if (messageKey.equals(record.key())) {

                                // Verificar payload y topic
                                Assertions.assertEquals(payload, record.value());
                                Assertions.assertEquals("booking-created", record.topic());

                                // Verificar que el header existe
                                Assertions.assertNotNull(record.headers().lastHeader("correlationId"));

                                // Verificar que conserva el valor original
                                Assertions.assertArrayEquals(
                                        originalHeaders.lastHeader("correlationId").value(),
                                        record.headers().lastHeader("correlationId").value()
                                );

                                received.set(true);
                            }

                        }
                        Assertions.assertTrue(received.get(), "No se ha encontrado el evento republicado en Kafka");
                    });
        } finally {
            consumer.close();
        }
    }

    @Test
    void shouldReturnEventToPendingWhenKafkaPublishingFails() {
        // Escenario: Kafka falla | Comportamiento esperado: PROCESSING → PENDING
        // GIVEN
        String messageKey = UUID.randomUUID().toString();
        String headers = "[]";
        String payload = """
                {
                    "integrationEventId": "%s",
                    "bookingId": "%s",
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 26L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);

        // Persistencia real en PostgreSQL
        StepVerifier.create(dataEventRecoveryRepository.save(event)).expectNextCount(1).verifyComplete();

        // Simular fallo de Kafka
        CompletableFuture<SendResult<String, String>> failedFuture =
                CompletableFuture.failedFuture(
                        new RuntimeException("Kafka broker unavailable")
                );

        Mockito.doReturn(failedFuture)
                .when(kafkaTemplate)
                .send(ArgumentMatchers.<ProducerRecord<String, String>>argThat(record ->
                        "booking-created".equals(record.topic())
                                && messageKey.equals(record.key())
                                && payload.equals(record.value())
                ));

        // WHEN
        StepVerifier.create(dltRecoveryService.reprocess()).verifyComplete();

        // THEN: consultar PostgreSQL real
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {

                    Assertions.assertEquals(RecoveryStatus.PENDING, recovered.getStatus());
                    Assertions.assertEquals(messageKey, recovered.getMessageKey());
                    Assertions.assertEquals(payload, recovered.getPayload());
                    Assertions.assertNull(recovered.getAttemptId());
                    Assertions.assertNull(recovered.getProcessingAt());
                    Assertions.assertNull(recovered.getRepublishedAt());

                })
                .verifyComplete();

        Mockito.verify(kafkaTemplate, Mockito.times(1))
                .send(ArgumentMatchers.any(ProducerRecord.class));
    }

    @Test
    void shouldNotPublishWhenOptimisticLockingFails() {
        // Escenario: Otro proceso ya ha reclamado el evento | Comportamiento esperado: No se publica
        // GIVEN
        String messageKey = UUID.randomUUID().toString();
        String payload = "{\"test\":\"optimistic-locking\"}";
        String headers = "[]";

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 27L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);

        // 1. Guardar el evento realmente
        dataEventRecoveryRepository.save(event).block();

        // 2. Recuperar una copia obsoleta de la entidad
        EventRecoveryEntity staleEvent = dataEventRecoveryRepository.findById(eventId).block();
        Assertions.assertNotNull(staleEvent);

        // 3. Otro proceso reclama el evento
        EventRecoveryEntity concurrentEvent = dataEventRecoveryRepository.findById(eventId).block();
        Assertions.assertNotNull(concurrentEvent);
        concurrentEvent.markAsProcessing(UUID.randomUUID());
        dataEventRecoveryRepository.save(concurrentEvent).block();

        // Ahora PostgreSQL tiene una versión superior.
        // staleEvent conserva la versión antigua y su estado PENDING.

        // 4. Simular que nuestro servicio había leído la versión obsoleta
        Mockito.doReturn(reactor.core.publisher.Flux.just(staleEvent))
                .when(dataEventRecoveryRepository)
                .findAllByStatus(RecoveryStatus.PENDING);

        // WHEN / THEN
        StepVerifier.create(dltRecoveryService.reprocess())
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        // El servicio NO debe publicar el evento
        Mockito.verify(kafkaTemplate, Mockito.never())
                .send(ArgumentMatchers.any(ProducerRecord.class));

        // Comprobar que la reclamación concurrente  permanece en PostgreSQL
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, recovered.getStatus());
                    Assertions.assertEquals(concurrentEvent.getAttemptId(), recovered.getAttemptId());
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenSavingRepublishedStatusFails() {
        //Escenario: Kafka confirma, pero falla PostgreSQL | Resultado esperado: Se propaga el error
        // GIVEN
        String messageKey = UUID.randomUUID().toString();
        String headers = "[]";
        String payload = """
                {
                    "integrationEventId": "%s",
                    "bookingId": "%s",
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 28L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);

        // Persistir PENDING realmente
        dataEventRecoveryRepository.save(event).block();

        // Simular fallo únicamente al guardar REPUBLISHED
        Mockito.doReturn(Mono.error(
                        new RuntimeException("PostgreSQL unavailable after Kafka ACK")
                ))
                .when(dataEventRecoveryRepository)
                .save(ArgumentMatchers.argThat(entity ->
                        entity != null
                                && eventId.equals(entity.getEventRecoveryEntityId())
                                && entity.getStatus() == RecoveryStatus.REPUBLISHED
                ));

        // Kafka funciona realmente. No interceptamos KafkaTemplate.send().
        // WHEN / THEN
        StepVerifier.create(dltRecoveryService.reprocess())
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains(
                                "PostgreSQL unavailable after Kafka ACK"
                        )
                )
                .verify();

        // Comprobar que Kafka recibió la petición
        Mockito.verify(kafkaTemplate, Mockito.times(1))
                .send(ArgumentMatchers.any(ProducerRecord.class));

        // Consultar PostgreSQL real
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {

                    // Se guardó PROCESSING,
                    // pero no se consiguió guardar REPUBLISHED.
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, recovered.getStatus());
                    Assertions.assertNotNull(recovered.getAttemptId());
                    Assertions.assertNotNull(recovered.getProcessingAt());
                    Assertions.assertNull(recovered.getRepublishedAt());
                })
                .verifyComplete();
    }
}

