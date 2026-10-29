package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * Comprueba Kafka → DLT → PostgreSQL.
 *
 * */
@SpringBootTest
@Testcontainers
public class DltRecoveryConsumerIT {


    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @Autowired
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryConsumerIT.class);

    @Test
    void shouldPersistFailedKafkaEventFromDlt() {

        // 1. Publicamos un evento en booking-created
        //                   ▼
        // 2. BookingCreatedKafkaConsumer recibe el evento
        //                   ▼
        // 3. Provocamos un error de procesamiento
        //                   ▼
        // 4. DefaultErrorHandler envía directamente a DLT por tratarse de JacksonException
        //    (errorHandler.addNotRetryableExceptions(JacksonException.class))
        //                   ▼
        // 5. DeadLetterPublishingRecoverer → booking-created.DLT
        //                   ▼
        // 6. DltRecoveryConsumer recibe el mensaje
        //                   ▼
        // 7. Guarda el evento en recovery_events
        //                   ▼
        // 8. Verificamos status = PENDING

        String jsonOk = "{" +
                " \"integrationEventId\":\"" + UUID.randomUUID() + "\"," +
                " \"bookingId\":\"" + UUID.randomUUID() + "\"," +
                " \"eventId\":\"" + UUID.randomUUID() + "\"," +
                " \"userId\":\"" + UUID.randomUUID() + "\"" +
                "}";

        String jsonKo = "{" +
                " \"integrationEventId\":\" NOT_AN_UUID \"" +
                "}";

        String messageKey = UUID.randomUUID().toString();

        kafkaTemplate
                .send(
                        "booking-created",
                        messageKey,
                        jsonKo
                )
                //Registra un callback y devuelve otro CompletableFuture sin bloquear.
                .thenAccept(result -> {
                    log.info("*****" + result.toString());
                })
                //Espera a que termine la operación y devuelve el resultado.
                .join();


        Awaitility.await()
                .atMost(Duration.ofMillis(15000L))
                .untilAsserted(() -> {
                    Flux<EventRecoveryEntity> all = dataEventRecoveryRepository.findAllByStatus(RecoveryStatus.PENDING);

                    List<EventRecoveryEntity> list = all.collectList().block();

                    Optional<EventRecoveryEntity> opt = list
                            .stream()
                            .filter(e -> e.getMessageKey().equals(messageKey))
                            .findFirst();

                    Assertions.assertTrue( opt.isPresent());

                    EventRecoveryEntity event = opt.orElseThrow();

                    Assertions.assertEquals(RecoveryStatus.PENDING, event.getStatus());
                    Assertions.assertEquals(jsonKo, event.getPayload());
                    Assertions.assertEquals("booking-created.DLT", event.getEventRecoveryEntityId().kafkaTopic());
                });


    }


}
