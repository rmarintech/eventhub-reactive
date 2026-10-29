package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;


import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;


/**
 *
 * Comprueba la recuperación ante fallos de PostgreSQL.
 * Spring R2DBC ───► Toxiproxy ───► PostgreSQL
 *
 *
 */
@SpringBootTest
@Testcontainers
class DltRecoveryRetryIT {


    static Network network = Network.newNetwork();

    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withNetwork(network)
                    .withNetworkAliases("postgres");

    static ToxiproxyContainer toxiproxy =
            new ToxiproxyContainer("shopify/toxiproxy:2.1.4")
                    .withNetwork(network);

    static {
        toxiproxy.addExposedPort(8666);
    }

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

//    @Autowired
//    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static Proxy postgresProxy;

    @MockitoSpyBean
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    static {
        try {
            postgres.start();
            toxiproxy.start();

            ToxiproxyClient client = new ToxiproxyClient(
                    toxiproxy.getHost(),
                    toxiproxy.getControlPort()
            );

            postgresProxy = client.createProxy(
                    "postgres",
                    "0.0.0.0:8666",
                    "postgres:5432"
            );

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://"
                        + toxiproxy.getHost()
                        + ":"
                        + toxiproxy.getMappedPort(8666)
                        + "/"
                        + postgres.getDatabaseName()
        );

        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Test
    void shouldConnectToPostgresThroughToxiproxy() {

        StepVerifier.create(dataEventRecoveryRepository.count())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldFailWhenPostgresConnectionIsCut() throws IOException {

        // 1. Comprobar que PostgreSQL está disponible
        StepVerifier.create(dataEventRecoveryRepository.count())
                .expectNextCount(1)
                .verifyComplete();

        // 2. Interrumpir la conexión
        Toxic toxic = postgresProxy.toxics()
                .timeout("postgres-down", ToxicDirection.DOWNSTREAM, 1000);
        try {
            // 3. Comprobar que PostgreSQL deja de responder
            StepVerifier.create(dataEventRecoveryRepository.count())
                    .expectError()
                    .verify(Duration.ofSeconds(15));
        } finally {
            toxic.remove();
        }

        // 4. Comprobar que PostgreSQL vuelve a responder
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {

                    StepVerifier.create(dataEventRecoveryRepository.count())
                            .expectNextCount(1)
                            .verifyComplete();
                });
    }

    @Test
    void shouldRetryDltEventWhenPostgresRecovers() throws Exception {

        String messageKey = UUID.randomUUID().toString();
        String payload = "{\"test\":\"postgres-recovery\"}";

        // 2. Simular caída de PostgreSQL
        Toxic toxic = postgresProxy.toxics()
                .timeout("postgres-down", ToxicDirection.DOWNSTREAM, 1000);
        try {
            // 3. Comprobar que PostgreSQL deja de responder
            StepVerifier.create(dataEventRecoveryRepository.count())
                    .expectError()
                    .verify(Duration.ofSeconds(15));

            Mockito.clearInvocations(dataEventRecoveryRepository);

            // 4. Publicar evento directamente en booking-created.DLT
            kafkaTemplate
                    .send(
                            "booking-created.DLT",
                            messageKey,
                            payload
                    )
                    .join();

            // 5. Verificar que el consumidor reintenta guardar el evento
            Mockito.verify(
                    dataEventRecoveryRepository,
                    Mockito.timeout(30000).atLeast(3)
            ).save(Mockito.any(EventRecoveryEntity.class));

        } finally {
            // 6. Restaurar PostgreSQL
            toxic.remove();
            System.out.println("POSTGRESQL CONNECTION RESTORED");
        }

        // 7. Comprobar que PostgreSQL vuelve a responder
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {

                    StepVerifier.create(dataEventRecoveryRepository.count())
                            .expectNextCount(1)
                            .verifyComplete();
                });

        // 8. Verificar que el evento acaba persistido como PENDING
        Awaitility.await()
                .atMost(Duration.ofSeconds(45))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {

                    List<EventRecoveryEntity> events =
                            dataEventRecoveryRepository
                                    .findAllByStatus(RecoveryStatus.PENDING)
                                    .collectList()
                                    .block();

                    Assertions.assertNotNull(events);

                    System.out.println("Eventos PENDING encontrados: " + events.size());
                    events.forEach(e ->
                            System.out.println(
                                    "Key: " + e.getMessageKey()
                                            + " | Payload: " + e.getPayload()
                                            + " | Status: " + e.getStatus()
                            )
                    );

                    System.out.println("MessageKey esperado: " + messageKey);
                    EventRecoveryEntity eventEntity = events
                            .stream()
                            .filter(e -> e.getMessageKey().equals(messageKey))
                            .findFirst()
                            .orElse(null);

                    Assertions.assertNotNull(
                            eventEntity,
                            "El evento todavía no está persistido"
                    );
                    Assertions.assertEquals(payload, eventEntity.getPayload());
                    Assertions.assertEquals(RecoveryStatus.PENDING, eventEntity.getStatus());

                });
    }
}
