package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.RetryableEventAvailabilityAdapter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class EventClientConfiguration {

    @Bean("eventRestClient")
    public RestClient eventRestClient(
            RestClient.Builder builder,
            @Value("${clients.event-service.base-url}") String baseUrl) {

        /** Timeout Configuration */

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public Retry eventAvailabilityRetry() {

        //        Intentos totales:	3
        //        Espera entre intentos:	500 ms
        //        Excepción reintentable:	EventServiceUnavailableException
        //        Excepción no reintentable:	EventNotFoundException
        //        Resultado false: 	No reintentar

        /** Retry Configuration */

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(EventServiceUnavailableException.class)
                .build();

        return Retry.of("eventAvailability", config);
    }

// Creamos un decorador encargado de gestionar los retry's y delegar las llamadas
//    @Bean
//    public EventAvailabilityPort eventAvailabilityPort(
//    @Qualifier("eventRestClient") RestClient restClient) {
//        return new RestEventAvailabilityAdapter(restClient);
//    }

    @Bean
    public EventAvailabilityPort eventAvailabilityPort(
            @Qualifier("eventRestClient") RestClient restClient,
            Retry eventAvailabilityRetry) {

        EventAvailabilityPort restAdapter = new RestEventAvailabilityAdapter(restClient);

        return new RetryableEventAvailabilityAdapter(restAdapter, eventAvailabilityRetry);
    }


}
