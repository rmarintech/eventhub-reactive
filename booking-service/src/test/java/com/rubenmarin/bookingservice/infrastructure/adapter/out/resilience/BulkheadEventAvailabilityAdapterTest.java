package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class BulkheadEventAvailabilityAdapterTest {

    @Mock
    EventAvailabilityPort delegate;

    private BulkheadEventAvailabilityAdapter adapter;
    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build();

        bulkhead = Bulkhead.of("eventAvailabilityTest", config);

        adapter = new BulkheadEventAvailabilityAdapter(
                delegate,
                bulkhead
        );
    }

    @Test
    void shouldRejectCallWhenBulkheadIsFull() throws Exception {

        EventId eventId = new EventId(UUID.randomUUID());

        // 1. Dos latches para controlar la ejecución concurrente.
        CountDownLatch enteredLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        // 2. Simulamos que Event Service tarda en responder.
        Mockito.when(delegate.isAvailable(eventId))
                .thenAnswer(invocation -> {

                    enteredLatch.countDown();

                    if (!releaseLatch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timeout waiting to release delegate");
                    }

                    return true;
                });

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // 3. Primera petición: se ejecuta en otro hilo.
            Future<Boolean> firstCall = executor.submit(() -> adapter.isAvailable(eventId));

            // Esperamos hasta que entre en el delegado.
            Assertions.assertTrue(
                    enteredLatch.await(5, TimeUnit.SECONDS),
                    "The first call did not enter the delegate"
            );

            // 4. La primera llamada sigue ocupando el único permiso. (.maxConcurrentCalls(1))
            Assertions.assertEquals(0,
                    bulkhead.getMetrics().getAvailableConcurrentCalls());

            // 5. Segunda petición: debe rechazarse inmediatamente.
            EventServiceUnavailableException ex =
                    Assertions.assertThrows(
                            EventServiceUnavailableException.class,
                            () -> adapter.isAvailable(eventId)
                    );

            // Comprobamos que el rechazo procede del Bulkhead.
            Assertions.assertInstanceOf(BulkheadFullException.class, ex.getCause());

            // 6. Liberamos la primera llamada.
            releaseLatch.countDown();

            Assertions.assertTrue(firstCall.get(5, TimeUnit.SECONDS));

            // 7. El permiso debe volver a estar disponible.
            Assertions.assertEquals(1, bulkhead.getMetrics().getAvailableConcurrentCalls());

            // 8. Solo 1 llamada EventAvailabilityPort.
            Mockito.verify(delegate, Mockito.times(1)).isAvailable(eventId);

        } finally {
            // Evita dejar bloqueado el hilo si falla una aserción.
            releaseLatch.countDown();
            executor.shutdownNow();
        }
    }
}
