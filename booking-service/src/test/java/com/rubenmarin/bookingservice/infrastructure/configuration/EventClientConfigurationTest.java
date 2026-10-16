package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.CircuitBreakerEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.RetryableEventAvailabilityAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
//@ContextConfiguration
class EventClientConfigurationTest {

    @Autowired
    EventAvailabilityPort eventAvailabilityPort;

    @Autowired
    RestClient eventRestClient;

    @Autowired
    Retry eventAvailabilityRetry;

    @Autowired
    CircuitBreaker eventAvailabilityCircuitBreaker;




    @Test
    void shouldLoadEventClientBeans() {

        assertNotNull(eventAvailabilityPort);
        assertNotNull(eventRestClient);
        assertNotNull(eventAvailabilityRetry);
        assertNotNull(eventAvailabilityCircuitBreaker);
    }


    // Evolución 1/3:
    // Verifica que Spring ya no inyecta el adaptador HTTP
    // directamente RestEventAvailabilityAdapter.
    @Test
    void shouldNotInjectHttpAdapter() {
        Assertions.assertFalse(eventAvailabilityPort instanceof RestEventAvailabilityAdapter);
    }

    // Evolución 2/3:
    // Verifica que Spring ya no inyecta el decorador Retryable.
    @Test
    void shouldNotInjectRetryDecorator() {
        Assertions.assertFalse(eventAvailabilityPort instanceof RetryableEventAvailabilityAdapter);
    }

    //Evolución 3/3:
    // Verifica que Spring inyecta realmente el decorador CircuitBreaker,
    // y no el adaptador HTTP directamente.
    @Test
    void shouldInjectCircuitBreakerDecorator() {

        Assertions.assertInstanceOf(
                CircuitBreakerEventAvailabilityAdapter.class,
                eventAvailabilityPort
        );
    }
}