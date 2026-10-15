package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import java.util.concurrent.*;

@SpringBootTest
@Testcontainers
class ConcurrentBookingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @MockitoBean
    EventAvailabilityPort eventAvailabilityPort;

    @Autowired
    BookingUseCase bookingUseCase;

    @Autowired
    BookingRepository repository;

    @Test
    void shouldAllowOnlyOneConcurrentBooking() throws Exception {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Mockito.when(eventAvailabilityPort.isAvailable(eventId)).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task = () -> {
            ready.countDown();
            start.await();

            try {
                bookingUseCase.createBooking(eventId, userId);
                return true;
            } catch (BookingAlreadyExistsException ex) {
                return false;
            }
        };

        try {
            Future<Boolean> result1 = executor.submit(task);
            Future<Boolean> result2 = executor.submit(task);

            // Esperamos a que ambos hilos estén preparados.
            Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS));

            // Permitimos que ambos intenten reservar.
            start.countDown();

            boolean success1 = result1.get(15, TimeUnit.SECONDS);
            boolean success2 = result2.get(15, TimeUnit.SECONDS);

            // Exactamente una petición debe tener éxito.
            Assertions.assertNotEquals(success1, success2);

            // Debe existir una reserva activa.
            Assertions.assertTrue(repository.existsActiveBooking(eventId, userId));

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}