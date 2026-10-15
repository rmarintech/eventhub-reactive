package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RetryableEventAvailabilityAdapterTest {

    @Mock
    EventAvailabilityPort delegate;

    private RetryableEventAvailabilityAdapter adapter;

    @BeforeEach
    void setUp() {

        //        Intentos totales:	3
        //        Espera entre intentos:	0 ms
        //        Excepción reintentable:	EventServiceUnavailableException
        //        Excepción no reintentable:	EventNotFoundException
        //        Resultado false: 	No reintentar

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .retryExceptions(EventServiceUnavailableException.class)
                .build();

        Retry retry = Retry.of("eventAvailabilityTest", config);

        adapter = new RetryableEventAvailabilityAdapter(delegate, retry);
    }

    @Test
    void shouldNotRetryWhenEventIsAvailable() {
        // 200	1 call return: true
        EventId eventId = new EventId(UUID.randomUUID());

        Mockito.when(delegate.isAvailable(eventId)).thenReturn(true);

        Assertions.assertTrue(adapter.isAvailable(eventId));

        Mockito.verify(delegate, Mockito.times(1)).isAvailable(eventId);
    }

    @Test
    void shouldNotRetryWhenEventIsNotAvailable() {
        // 200	1 call return: true
        EventId eventId = new EventId(UUID.randomUUID());

        Mockito.when(delegate.isAvailable(eventId)).thenReturn(false);

        Assertions.assertFalse(adapter.isAvailable(eventId));

        Mockito.verify(delegate, Mockito.times(1)).isAvailable(eventId);
    }

    @Test
    void shouldRetryTwiceAndEventuallySucceed() {
        // 503 → 503 → 200	3 calls return: true
        EventId eventId = new EventId(UUID.randomUUID());

        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")))
                .thenReturn(true);

        Assertions.assertTrue(adapter.isAvailable(eventId));

        Mockito.verify(delegate, Mockito.times(3)).isAvailable(eventId);
    }


    @Test
    void shouldThrowExceptionAfterThreeFailedAttempts() {
        // 503 → 503 → 503	3 calls return: EventServiceUnavailableException
        EventId eventId = new EventId(UUID.randomUUID());

        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventServiceUnavailableException(new RuntimeException("503")));

        Assertions.assertThrows(EventServiceUnavailableException.class, () -> adapter.isAvailable(eventId));

        Mockito.verify(delegate, Mockito.times(3)).isAvailable(eventId);
    }

    @Test
    void shouldNotRetryWhenEventDoesNotExist() {
        // 404 1 call return: EventNotFoundException
        EventId eventId = new EventId(UUID.randomUUID());

        Mockito.when(delegate.isAvailable(eventId))
                .thenThrow(new EventNotFoundException(eventId, new RuntimeException("404")));

        Assertions.assertThrows(EventNotFoundException.class, () -> adapter.isAvailable(eventId));

        Mockito.verify(delegate, Mockito.times(1)).isAvailable(eventId);
    }
}
