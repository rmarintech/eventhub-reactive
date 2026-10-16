package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerEventAvailabilityAdapterTest {

    @Mock
    EventAvailabilityPort delegate;
    private CircuitBreakerEventAvailabilityAdapter adapter;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .recordExceptions(EventServiceUnavailableException.class)
                .ignoreExceptions(EventNotFoundException.class)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        circuitBreaker = CircuitBreaker.of("eventAvailabilityTest", config);

        adapter = new CircuitBreakerEventAvailabilityAdapter(
                delegate,
                circuitBreaker
        );
    }

    @Test
    void shouldIgnoreEventNotFoundException() {
        // CLOSED → 3 excepciones ignoradas → CLOSED
        EventId eventId = new EventId(UUID.randomUUID());

        // 1. Configurar Mockito para que el delegado falle.
        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventNotFoundException(eventId, new RuntimeException("404"))
                );

        // 2. Primera llamada: comprobar excepción.
        //    El circuito debe seguir CLOSED.
        Assertions.assertThrows(
                EventNotFoundException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 3. Segunda llamada: comprobar excepción.
        //    El circuito debe seguir CLOSED.
        Assertions.assertThrows(
                EventNotFoundException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 3. Tercera llamada: comprobar excepción.
        //    El circuito debe seguir CLOSED.
        Assertions.assertThrows(
                EventNotFoundException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 4. Verificar que el delegado solamente recibió 2 llamadas.
        Mockito.verify(delegate, Mockito.times(3)).isAvailable(eventId);

        //5. Verificar que las excepciones ignoradas tampoco aparecen en las métricas de éxitos o fallos.
        Assertions.assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
        Assertions.assertEquals(0, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    void shouldOpenCircuitAfterTwoFailures() {
        // CLOSED, fail -> OPEN, fail -> rejected by cb
        EventId eventId = new EventId(UUID.randomUUID());

        // 1. Configurar Mockito para que el delegado falle.
        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503"))
                );

        // 2. Primera llamada: comprobar excepción.
        //    El circuito debe seguir CLOSED.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 3. Segunda llamada: comprobar excepción.
        //    El circuito debe pasar a OPEN.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        // 4. Tercera llamada: comprobar excepción.
        //    Debe ser rechazada por Circuit Breaker (CallNotPermittedException).
        EventServiceUnavailableException ex =
                Assertions.assertThrows(
                        EventServiceUnavailableException.class,
                        () -> adapter.isAvailable(eventId)
                );
        Assertions.assertInstanceOf(CallNotPermittedException.class, ex.getCause());

        // 5. Verificar que el delegado solamente recibió 2 llamadas.
        Mockito.verify(delegate, Mockito.times(2)).isAvailable(eventId);
    }

    @Test
    void shouldCloseCircuitAfterSuccessfulRecovery() {
        // CLOSED → 2 fallos → OPEN
        // OPEN → transición manual → HALF_OPEN
        // HALF_OPEN → 2 éxitos → CLOSED

        EventId eventId = new EventId(UUID.randomUUID());

        // 1. Configurar Mockito para que el delegado falle, falle, éxito.
        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenReturn(true);

        // 2. Primera llamada: comprobar excepción.
        //    El circuito debe seguir CLOSED.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );
        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 3. Segunda llamada: comprobar excepción.
        //    El circuito debe pasar a OPEN.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );
        Assertions.assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        // 4. Forzamos el paso a half-open sin esperar.
        //    El circuito debe pasar a HALF_OPEN.
        circuitBreaker.transitionToHalfOpenState();
        Assertions.assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreaker.getState()
        );

        // 5. Tercera llamada: primer éxito en HALF_OPEN.
        //    El circuito debe seguir en HALF_OPEN.
        Assertions.assertTrue(adapter.isAvailable(eventId));
        Assertions.assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreaker.getState()
        );


        // 6. Cuarta llamada: segundo éxito, transición a CLOSED.
        //    El circuito debe pasar a CLOSED (.permittedNumberOfCallsInHalfOpenState(2)).
        Assertions.assertTrue(adapter.isAvailable(eventId));
        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 7. Verificar que el delegado recibió exactamente 4 llamadas.
        Mockito.verify(delegate, Mockito.times(4)).isAvailable(eventId);


    }

    @Test
    void shouldReopenCircuitWhenRecoveryFails() {
// HALF_OPEN → OPEN.
        EventId eventId = new EventId(UUID.randomUUID());

        // 1. Dos fallos iniciales para abrir el circuito.
        // En HALF_OPEN: un fallo y un éxito (50% de fallos).
        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenReturn(true);

        // 2. Primer fallo: el circuito sigue CLOSED.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        // 3. Segundo fallo: el circuito pasa a OPEN.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        // 4. Forzamos HALF_OPEN sin esperar 10 segundos.
        circuitBreaker.transitionToHalfOpenState();

        Assertions.assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreaker.getState()
        );

        // 5. Primera llamada de recuperación: falla.
        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreaker.getState()
        );

        // 6. Segunda llamada de recuperación: tiene éxito.
        Assertions.assertTrue(adapter.isAvailable(eventId));

        // 1 fallo de 2 llamadas = 50%.
        // Al alcanzar el umbral configurado, vuelve a OPEN.
        Assertions.assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        // 7. Nueva petición: rechazada directamente por Circuit Breaker.
        EventServiceUnavailableException ex = Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertInstanceOf(
                CallNotPermittedException.class,
                ex.getCause()
        );

        // 8. Solo se ejecutaron las 4 llamadas permitidas.
        Mockito.verify(delegate, Mockito.times(4)).isAvailable(eventId);

    }

}
