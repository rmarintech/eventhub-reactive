package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.RetryableEventAvailabilityAdapter;
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
    Retry eventAvailabilityRetry;

    @Autowired
    RestClient eventRestClient;

    @Autowired
    EventAvailabilityPort eventAvailabilityPort;


    @Test
    void shouldLoadEventClientBeans() {

        assertNotNull(eventAvailabilityRetry);
        assertNotNull(eventRestClient);
        assertNotNull(eventAvailabilityPort);
    }


    // Verifica que Spring inyecta realmente nuestro decorador,
    // y no el adaptador HTTP directamente.
    @Test
    void shouldInjectRetryDecorator() {

        Assertions.assertInstanceOf(
                RetryableEventAvailabilityAdapter.class,
                eventAvailabilityPort
        );
    }
}