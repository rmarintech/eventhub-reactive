package com.rubenmarin.eventhub.event.infrastructure.recovery;

import com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka.KafkaHeadersCodec;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntityId;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests de integración de la recuperación de eventos desde la DLT.
 *
 * Se usan PostgreSQL y Kafka reales en Testcontainers. Los spies permiten
 * inyectar fallos concretos sin simular toda la infraestructura.
 *
 * NOTA: se conserva la lógica y las expectativas de los tests originales.
 */
@SpringBootTest
@Testcontainers
class DltRecoveryServiceIT {

    // PostgreSQL almacena el estado PENDING / PROCESSING / REPUBLISHED.
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // Broker Kafka real: las pruebas pueden verificar entregas efectivas.
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    // Spy: usa el repositorio real, pero permite forzar fallos o lecturas.
    @MockitoSpyBean
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private DltRecoveryService dltRecoveryService;

    @Autowired
    private KafkaHeadersCodec kafkaHeadersCodec;

    // Spy: KafkaTemplate real salvo cuando un test configura una respuesta falsa.
    @MockitoSpyBean(name = "kafkaTemplate")
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryServiceIT.class);
    private static final String DLT_TOPIC = "booking-created.DLT";

    @Test
    void shouldRepublishPendingEventSuccessfully() {
        // ESCENARIO: recuperación normal sin errores.
        // GIVEN: mensaje fallido almacenado como PENDING en PostgreSQL.
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = 25L;

        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        // La cabecera original debe sobrevivir a la persistencia y republicación.
        Headers originalHeaders = new RecordHeaders()
                .add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        // integrationEventId conserva la identidad lógica para deduplicación Inbox.
        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        // Consumidor independiente del servicio: comprueba el mensaje en el broker.
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

            // WHEN: guardar PENDING -> reprocess() -> leer estado final de BBDD.
            // then(...) encadena las operaciones, descartando el resultado previo.
            Mono<EventRecoveryEntity> operation =
                    dataEventRecoveryRepository.save(eventRecoveryEntity)
                            .then(dltRecoveryService.reprocess())
                            .then(dataEventRecoveryRepository.findById(eventRecoveryEntityId));

            // THEN: publicación exitosa implica estado REPUBLISHED y metadatos intactos.
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

            // Verificación independiente: no basta con confiar en el estado de PostgreSQL.
            // Awaitility espera de manera acotada hasta observar la entrega en Kafka.
            AtomicBoolean received = new AtomicBoolean(false);
            Awaitility.await()
                    .atMost(Duration.ofSeconds(20))
                    .pollInterval(Duration.ofMillis(500))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                        for (ConsumerRecord<String, String> record : records) {
                            if (messageKey.equals(record.key())) {
                                Assertions.assertEquals(payload, record.value());
                                Assertions.assertEquals("booking-created", record.topic());
                                Assertions.assertNotNull(record.headers().lastHeader("correlationId"));
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
            // Evita dejar consumidores y conexiones abiertas tras el test.
            consumer.close();
        }
    }

    @Test
    void shouldReturnEventToPendingWhenKafkaPublishingFails() {
        // ESCENARIO: Kafka rechaza el envío.
        // EXPECTED: PROCESSING -> PENDING, sin marcar REPUBLISHED.
        String messageKey = UUID.randomUUID().toString();
        String headers = "[]";
        String payload = """
                {
                    "integrationEventId": "%s",
                    "bookingId": "%s",
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 26L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);

        // Persistencia real de partida.
        StepVerifier.create(dataEventRecoveryRepository.save(event)).expectNextCount(1).verifyComplete();

        // Inyectar fallo asíncrono solamente en el KafkaTemplate.send() relevante.
        CompletableFuture<SendResult<String, String>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka broker unavailable"));
        Mockito.doReturn(failedFuture)
                .when(kafkaTemplate)
                .send(ArgumentMatchers.<ProducerRecord<String, String>>argThat(record ->
                        "booking-created".equals(record.topic())
                                && messageKey.equals(record.key())
                                && payload.equals(record.value())
                ));

        // El error de envío se maneja recuperando el estado PENDING.
        StepVerifier.create(dltRecoveryService.reprocess()).verifyComplete();

        // Comprobar efectos persistidos tras la recuperación del fallo.
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

        // Verifica una invocación del productor; no que Kafka haya aceptado el mensaje.
        Mockito.verify(kafkaTemplate, Mockito.times(1))
                .send(ArgumentMatchers.any(ProducerRecord.class));
    }

    @Test
    void shouldNotPublishWhenOptimisticLockingFails() {
        // ESCENARIO: dos lectores parten de la misma versión y otro proceso gana.
        // EXPECTED original: el save de la copia obsoleta falla y no se publica.
        String messageKey = UUID.randomUUID().toString();
        String payload = "{\"test\":\"optimistic-locking\"}";
        String headers = "[]";

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 27L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);
        dataEventRecoveryRepository.save(event).block();

        // Snapshot antiguo: versión anterior, estado PENDING.
        EventRecoveryEntity staleEvent = dataEventRecoveryRepository.findById(eventId).block();
        Assertions.assertNotNull(staleEvent);

        // Simulación de otro proceso que reclama y persiste PROCESSING.
        EventRecoveryEntity concurrentEvent = dataEventRecoveryRepository.findById(eventId).block();
        Assertions.assertNotNull(concurrentEvent);
        concurrentEvent.markAsProcessing(UUID.randomUUID());
        dataEventRecoveryRepository.save(concurrentEvent).block();

        // El spy entrega la copia antigua al servicio para forzar la colisión.
        Mockito.doReturn(Flux.just(staleEvent))
                .when(dataEventRecoveryRepository)
                .findAllByStatus(RecoveryStatus.PENDING);

        StepVerifier.create(dltRecoveryService.reprocess())
                .verifyComplete();

        Mockito.verify(kafkaTemplate, Mockito.never())
                .send(ArgumentMatchers.any(ProducerRecord.class));

        // El estado persistido debe seguir perteneciendo al proceso ganador.
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, recovered.getStatus());
                    Assertions.assertEquals(concurrentEvent.getAttemptId(), recovered.getAttemptId());
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorWhenSavingRepublishedStatusFails() {
        // ESCENARIO: Kafka ACK correcto, pero falla PostgreSQL al guardar REPUBLISHED.
        // El mensaje puede haberse publicado; no debe fingirse que falló Kafka.
        String messageKey = UUID.randomUUID().toString();
        String headers = "[]";
        String payload = """
                {
                    "integrationEventId": "%s",
                    "bookingId": "%s",
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 28L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, headers, payload);
        dataEventRecoveryRepository.save(event).block();

        // Spy: solamente el guardado de REPUBLISHED fallará.
        Mockito.doReturn(Mono.error(new RuntimeException("PostgreSQL unavailable after Kafka ACK")))
                .when(dataEventRecoveryRepository)
                .save(ArgumentMatchers.argThat(entity ->
                        entity != null
                                && eventId.equals(entity.getEventRecoveryEntityId())
                                && entity.getStatus() == RecoveryStatus.REPUBLISHED
                ));

        // No se falsea el envío Kafka: se prueba la separación entre ambos errores.
        StepVerifier.create(dltRecoveryService.reprocess())
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains("PostgreSQL unavailable after Kafka ACK")
                )
                .verify();

        Mockito.verify(kafkaTemplate, Mockito.times(1))
                .send(ArgumentMatchers.any(ProducerRecord.class));

        // PROCESSING permanece porque falló la persistencia del paso final.
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, recovered.getStatus());
                    Assertions.assertNotNull(recovered.getAttemptId());
                    Assertions.assertNotNull(recovered.getProcessingAt());
                    Assertions.assertNull(recovered.getRepublishedAt());
                })
                .verifyComplete();
    }

    @Test
    void shouldPublishOnlyOnceWhenTwoReprocessExecutionsCompete() {
        // ESCENARIO: dos ejecuciones concurrentes reciben la misma versión PENDING.
        // EXPECTED original: solo la reclamación ganadora publica en Kafka.
        String messageKey = UUID.randomUUID().toString();
        String payload = """
                {
                    "integrationEventId": "%s",
                    "bookingId": "%s",
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        EventRecoveryEntityId eventId = new EventRecoveryEntityId(DLT_TOPIC, 0, 29L);
        EventRecoveryEntity event = new EventRecoveryEntity(eventId, messageKey, "[]", payload);
        dataEventRecoveryRepository.save(event).block();

        // Dos instancias independientes conservan la misma versión inicial.
        EventRecoveryEntity snapshotA = dataEventRecoveryRepository.findById(eventId).block();
        EventRecoveryEntity snapshotB = dataEventRecoveryRepository.findById(eventId).block();
        Assertions.assertNotNull(snapshotA);
        Assertions.assertNotNull(snapshotB);

        // La barrera impide que una lectura avance antes de que llegue la otra.
        CountDownLatch bothReadsCompleted = new CountDownLatch(2);
        AtomicInteger reads = new AtomicInteger();

        Mockito.doAnswer(invocation ->
                        Flux.defer(() -> {
                            int currentRead = reads.incrementAndGet();
                            if (currentRead > 2) {
                                return Flux.error(new IllegalStateException("Unexpected repository read"));
                            }
                            bothReadsCompleted.countDown();
                            try {
                                if (!bothReadsCompleted.await(10, TimeUnit.SECONDS)) {
                                    return Flux.error(new IllegalStateException("Concurrent reads timed out"));
                                }
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return Flux.error(ex);
                            }
                            return Flux.just(currentRead == 1 ? snapshotA : snapshotB);
                        }).subscribeOn(Schedulers.boundedElastic())
                ).when(dataEventRecoveryRepository)
                .findAllByStatus(RecoveryStatus.PENDING);

        // subscribeOn permite ejecutar ambas cadenas en hilos distintos.
        Mono<Void> executionA = dltRecoveryService.reprocess().subscribeOn(Schedulers.boundedElastic());
        Mono<Void> executionB = dltRecoveryService.reprocess().subscribeOn(Schedulers.boundedElastic());

        // Cada error individual se transforma en completion para poder esperar
        // a las dos ejecuciones y comprobar cuántas fallaron.
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        Mono<Void> concurrentExecution = Mono.when(
                executionA.onErrorResume(ex -> {
                    errors.add(ex);
                    return Mono.empty();
                }),
                executionB.onErrorResume(ex -> {
                    errors.add(ex);
                    return Mono.empty();
                })
        );

        StepVerifier.create(concurrentExecution).verifyComplete();
        Assertions.assertEquals(2, reads.get());

        // NOTA: estas expectativas también podrían estar desactualizadas si
        // reprocess() absorbe ahora los conflictos optimistas de cada evento.
        Assertions.assertEquals(0, errors.size());

        // Únicamente la ejecución ganadora debe llamar a KafkaTemplate.send().
        Mockito.verify(kafkaTemplate, Mockito.times(1))
                .send(ArgumentMatchers.<ProducerRecord<String, String>>argThat(record ->
                        record != null
                                && "booking-created".equals(record.topic())
                                && messageKey.equals(record.key())
                                && payload.equals(record.value())
                ));

        // Estado final leído de PostgreSQL real, no de snapshots en memoria.
        StepVerifier.create(dataEventRecoveryRepository.findById(eventId))
                .assertNext(recovered -> {
                    Assertions.assertEquals(RecoveryStatus.REPUBLISHED, recovered.getStatus());
                    Assertions.assertNotNull(recovered.getAttemptId());
                    Assertions.assertNotNull(recovered.getProcessingAt());
                    Assertions.assertNotNull(recovered.getRepublishedAt());
                })
                .verifyComplete();
    }
}
