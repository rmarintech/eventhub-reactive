package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import com.rubenmarin.paymentservice.domain.model.Payment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@SpringBootTest
@Testcontainers
class JpaInboxRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private InboxRepository inboxRepository;

    @Autowired
    private SpringDataInboxRepository springDataInboxRepository;

    @Test
    void shouldRegisterNewInboxEventInDatabase() {

        UUID integrationEventId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        InboxEvent inboxEvent = new InboxEvent(integrationEventId, receivedAt);

        boolean registered = inboxRepository.register(inboxEvent);
        Assertions.assertTrue(registered);

        boolean exists = springDataInboxRepository.existsById(integrationEventId);
        Assertions.assertTrue(exists);

    }

    @Test
    void shouldReturnFalseWhenInboxEventAlreadyExistsInDatabase() {

        UUID integrationEventId = UUID.randomUUID();
        Instant receivedAt1 = Instant.now().truncatedTo(ChronoUnit.MICROS);
        InboxEvent inboxEvent1 = new InboxEvent(integrationEventId, receivedAt1);


        boolean registered = inboxRepository.register(inboxEvent1);
        Assertions.assertTrue(registered);

        boolean exists = springDataInboxRepository.existsById(integrationEventId);
        Assertions.assertTrue(exists);

        Instant receivedAt2 = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MICROS);
        InboxEvent inboxEvent2 = new InboxEvent(integrationEventId, receivedAt2);
        boolean registeredAgain = inboxRepository.register(inboxEvent2);
        Assertions.assertFalse(registeredAgain);

        InboxJpaEntity inboxEntity = springDataInboxRepository.findById(integrationEventId).orElseThrow();
        Assertions.assertEquals(receivedAt1, inboxEntity.getReceivedAt());
    }

    @Test
    void shouldRegisterSameInboxEventOnlyOnceUnderConcurrency() {

        UUID integrationEventId = UUID.randomUUID();
        Instant receivedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        InboxEvent inboxEvent = new InboxEvent(integrationEventId, receivedAt);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {

            CountDownLatch startLatch = new CountDownLatch(1);

            Callable<Boolean> task1 = () -> {
                startLatch.await();
                return inboxRepository.register(inboxEvent);
            };

            Callable<Boolean> task2 = () -> {
                startLatch.await();
                return inboxRepository.register(inboxEvent);
            };

            Future<Boolean> future1 = executorService.submit(task1);
            Future<Boolean> future2 = executorService.submit(task2);


            // Liberar los dos hilos después de enviar ambas tareas.
            startLatch.countDown();

            List<Boolean> registeredList = new ArrayList<>();
            try {
                Boolean registered1 = future1.get(10, TimeUnit.SECONDS);
                registeredList.add(registered1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            try {
                Boolean registered2 = future2.get(10, TimeUnit.SECONDS);
                registeredList.add(registered2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertEquals(2, registeredList.size());
            Assertions.assertEquals(1L, registeredList.stream().filter(r -> r == true).count());
            Assertions.assertEquals(1L, registeredList.stream().filter(r -> r == false).count());

            boolean exists = springDataInboxRepository.existsById(integrationEventId);
            Assertions.assertTrue(exists);
        }


    }
}
