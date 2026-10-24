
package com.rubenmarin.bookingservice.infrastructure.adapter.out.ibmmq;

import com.ibm.mq.testcontainers.MQContainer;
import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jms.listener.auto-startup=false"
})
@Testcontainers
class IbmMqContainerIT {

    private static final String BOOKING_QUEUE = "BOOKING.CREATED.Q";
    private static final String BACKOUT_QUEUE = "BOOKING.BACKOUT.Q";

    private static final Pattern QUEUE_DEPTH_PATTERN =
            Pattern.compile("CURDEPTH\\((\\d+)\\)");

    @Container
    static final MQContainer ibmMq =
            new MQContainer(MQContainer.DEFAULT_IMAGE)
                    .acceptLicense()
                    .withQueueManager("QM1")
                    .withAppPassword("passw0rd")
                    .withEnv("MQ_CONNAUTH_USE_HTP", "true")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    Path.of("../ibmmq/init.mqsc")
                                            .toAbsolutePath()
                            ),
                            "/etc/mqm/20-init.mqsc"
                    );

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("bookingdb")
                    .withUsername("booking")
                    .withPassword("booking_test");

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JmsListenerEndpointRegistry jmsListenerRegistry;

    @DynamicPropertySource
    static void configureInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("ibm.mq.connName", ibmMq::getConnName);
        registry.add("ibm.mq.queueManager", ibmMq::getQueueManager);
        registry.add("ibm.mq.channel", ibmMq::getChannel);
        registry.add("ibm.mq.user", ibmMq::getAppUser);
        registry.add("ibm.mq.password", ibmMq::getAppPassword);

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldStartInfrastructure() {
        assertTrue(ibmMq.isRunning());
        assertTrue(postgres.isRunning());
        assertTrue(ibmMq.getPort() > 0);
        assertTrue(postgres.getMappedPort(5432) > 0);
    }

    @Test
    void shouldCreateBookingQueues() throws Exception {
        assertQueueExists(BOOKING_QUEUE);
        assertQueueExists(BACKOUT_QUEUE);
    }

    @Test
    void shouldPublishAndReceiveMessage() {
        String payload = "Test IBM MQ";

        jmsTemplate.setReceiveTimeout(5000);

        jmsTemplate.convertAndSend(BOOKING_QUEUE, payload);

        Object received = jmsTemplate.receiveAndConvert(BOOKING_QUEUE);

        assertNotNull(received);
        assertEquals(payload, received);
    }

    @Test
    void shouldConsumeBookingCreatedAndPersistInbox() {
        UUID integrationEventId = UUID.randomUUID();

        BookingCreatedIntegrationEvent event =
                new BookingCreatedIntegrationEvent(
                        integrationEventId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        String payload = objectMapper.writeValueAsString(event);

        assertEquals(0L, countInboxEvents(integrationEventId));

        jmsListenerRegistry.start();

        try {
            jmsTemplate.convertAndSend(BOOKING_QUEUE, payload);

            await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() ->
                            assertEquals(1L, countInboxEvents(integrationEventId))
                    );

        } finally {
            jmsListenerRegistry.stop();
        }
    }

    @Test
    void shouldIgnoreDuplicateBookingCreatedEvent() {
        UUID integrationEventId = UUID.randomUUID();

        BookingCreatedIntegrationEvent event =
                new BookingCreatedIntegrationEvent(
                        integrationEventId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        String payload = objectMapper.writeValueAsString(event);

        assertEquals(0L, countInboxEvents(integrationEventId));

        jmsListenerRegistry.start();

        try {
            // Primera entrega: inserción en Inbox.
            jmsTemplate.convertAndSend(BOOKING_QUEUE, payload);

            await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        assertEquals(1L, countInboxEvents(integrationEventId));
                        assertEquals(0, getQueueDepth(BOOKING_QUEUE));
                    });

            // Segunda entrega: mismo integrationEventId.
            jmsTemplate.convertAndSend(BOOKING_QUEUE, payload);

            // Esperar a que la cola vuelva a estar vacía.
            await()
                    .atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        assertEquals(0, getQueueDepth(BOOKING_QUEUE));
                        assertEquals(0, getQueueDepth(BACKOUT_QUEUE));
                        assertEquals(1L, countInboxEvents(integrationEventId));
                    });

        } finally {
            jmsListenerRegistry.stop();
        }
    }

    private long countInboxEvents(UUID bookingId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM inbox_events
                WHERE integration_event_id = ?
                """,
                Long.class,
                bookingId
        );

        return count == null ? 0L : count;
    }

    private int getQueueDepth(String queueName) throws Exception {
        var result = ibmMq.execInContainer(
                "sh",
                "-c",
                "echo \"DISPLAY QLOCAL('" + queueName +
                        "') CURDEPTH\" | runmqsc QM1"
        );

        assertEquals(
                0,
                result.getExitCode(),
                result.getStdout() + result.getStderr()
        );

        Matcher matcher =
                QUEUE_DEPTH_PATTERN.matcher(result.getStdout());

        assertTrue(
                matcher.find(),
                "No se pudo obtener CURDEPTH de " +
                        queueName + "\n" + result.getStdout()
        );

        return Integer.parseInt(matcher.group(1));
    }

    private void assertQueueExists(String queueName) throws Exception {
        var result = ibmMq.execInContainer(
                "sh",
                "-c",
                "echo \"DISPLAY QLOCAL('" + queueName +
                        "')\" | runmqsc QM1"
        );

        assertEquals(
                0,
                result.getExitCode(),
                result.getStdout() + result.getStderr()
        );

        assertTrue(
                result.getStdout().contains("QUEUE(" + queueName + ")"),
                "No se encontró la cola " +
                        queueName + "\n" + result.getStdout()
        );
    }
}
