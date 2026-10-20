
package com.rubenmarin.bookingservice.application.service;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.port.in.ProcessBookingCreatedUseCase;
import com.rubenmarin.bookingservice.application.port.out.InboxRepository;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa.SpringDataInboxRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import java.util.concurrent.*;


@SpringBootTest
@Testcontainers
class ProcessBookingCreatedServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    ProcessBookingCreatedUseCase useCase;

    @Autowired
    SpringDataInboxRepository springDataInboxRepository;

    @MockitoSpyBean
    InboxRepository inboxRepository;

    @Test
    void shouldRejectConcurrentDuplicateInboxEvents() throws Exception {

        UUID bookingId = UUID.randomUUID();

        BookingCreatedIntegrationEvent event =
                new BookingCreatedIntegrationEvent(
                        bookingId,
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        // Sincronizamos ambos hilos después de consultar el Inbox.
        CountDownLatch bothChecked = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {

            boolean exists = (Boolean) invocation.callRealMethod();

            bothChecked.countDown();

            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timeout waiting for concurrent consumers"
                );
            }

            return exists;

        }).when(inboxRepository).existsById(bookingId);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Throwable> task = () -> {
                try {
                    useCase.process(event);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            };

            Future<Throwable> first = executor.submit(task);
            Future<Throwable> second = executor.submit(task);

            // Ambos han consultado el Inbox antes de continuar.
            Assertions.assertTrue(
                    bothChecked.await(10, TimeUnit.SECONDS),
                    "Both consumers must check the Inbox"
            );

            // Permitimos que ambos intenten insertar.
            release.countDown();

            Throwable firstError = first.get(20, TimeUnit.SECONDS);
            Throwable secondError = second.get(20, TimeUnit.SECONDS);

            // Una transacción debe completar correctamente.
            long successful = 0;

            if (firstError == null) successful++;
            if (secondError == null) successful++;

            Assertions.assertEquals(1, successful);

            // La otra debe fallar por la PK del Inbox.
            Throwable error =
                    firstError != null ? firstError : secondError;

            Assertions.assertNotNull(error);
            Assertions.assertTrue(isInboxDuplicate(error));

            // PostgreSQL conserva exactamente un registro.
            long stored = springDataInboxRepository.findAll()
                    .stream()
                    .filter(entity -> entity.getEventId().equals(bookingId))
                    .count();

            Assertions.assertEquals(1, stored);

        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private boolean isInboxDuplicate(Throwable error) {

        Throwable cause = error;

        while (cause != null) {

            if (cause instanceof PSQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && sqlException.getServerErrorMessage() != null
                    && "inbox_events_pkey".equals(
                    sqlException.getServerErrorMessage()
                            .getConstraint())) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
