package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.BulkheadEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.CircuitBreakerEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.RetryableEventAvailabilityAdapter;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EventClientConfigurationTest {

    @Autowired
    EventAvailabilityPort eventAvailabilityPort;

    @Autowired
    RestClient eventRestClient;

    @Autowired
    Retry eventAvailabilityRetry;

    @Autowired
    CircuitBreaker eventAvailabilityCircuitBreaker;

    @Autowired
    Bulkhead eventAvailabilityBulkhead;


    @Test
    void shouldLoadEventClientBeans() {

        assertNotNull(eventAvailabilityPort);
        assertNotNull(eventRestClient);
        assertNotNull(eventAvailabilityRetry);
        assertNotNull(eventAvailabilityCircuitBreaker);
        assertNotNull(eventAvailabilityBulkhead);
    }


    // Evolución 1/4:
    // Verifica que Spring ya no inyecta el adaptador HTTP
    // directamente RestEventAvailabilityAdapter.
    @Test
    void shouldNotInjectHttpAdapter() {
        Assertions.assertFalse(
                eventAvailabilityPort
                        instanceof
                        RestEventAvailabilityAdapter
        );
    }

    // Evolución 2/4:
    // Verifica que Spring ya no inyecta el decorador Retryable.
    @Test
    void shouldNotInjectRetryDecorator() {
        Assertions.assertFalse(
                eventAvailabilityPort
                        instanceof
                        RetryableEventAvailabilityAdapter
        );
    }

    //Evolución 3/4:
    // Verifica que Spring ya no inyecta el decorador CircuitBreaker,
    @Test
    void shouldInjectCircuitBreakerDecorator() {
        Assertions.assertFalse(
                eventAvailabilityPort
                        instanceof
                        CircuitBreakerEventAvailabilityAdapter
        );
    }

    //Evolución 4/4:
    // Verifica que Spring inyecta realmente el decorador Bulkhead,
    // y no el adaptador HTTP directamente.
    @Test
    void shouldInjectBulkheadDecorator() {
        Assertions.assertInstanceOf(
                BulkheadEventAvailabilityAdapter.class,
                eventAvailabilityPort
        );
    }
}