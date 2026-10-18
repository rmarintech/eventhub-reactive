package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.http.RestEventCapacityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.BulkheadEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.CircuitBreakerEventAvailabilityAdapter;
import com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience.RetryableEventAvailabilityAdapter;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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

/**
    BookingService
            ↓
    EventAvailabilityPort
            ↓
    Bulkhead (máximo 5 operaciones)
            ↓
    Circuit Breaker
            ↓
    Retry (máximo 3 intentos)
            ↓
    RestClient (timeouts)
            ↓
    Event Service
*/


    @Bean
    public Retry eventAvailabilityRetry() {

        /** Retry Configuration
         *
         * Intentos totales:	        3.
         * Espera entre intentos:	    500 ms.
         * Excepción reintentable:	    EventServiceUnavailableException.
         * Excepción no reintentable:	EventNotFoundException.
         * Resultado false: 	        No reintentar.
         *
         */

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(EventServiceUnavailableException.class)
                .build();

        return Retry.of("eventAvailability", config);
    }

    @Bean
    public CircuitBreaker eventAvailabilityCircuitBreaker() {
        /** Circuit breaker Configuration
         *
         * failureRateThreshold(50)	                Abre el circuito con un 50 % o más de fallos.
         * slidingWindowSize(10)	                Considera las últimas 10 llamadas.
         * minimumNumberOfCalls(10)	                No evalúa el porcentaje hasta registrar 10 llamadas.
         * waitDurationInOpenState(10s)	            Espera 10 segundos antes de permitir pruebas de recuperación.
         * permittedNumberOfCallsInHalfOpenState(2)	Permite 2 llamadas de prueba.
         * recordExceptions(...)	                Contabiliza los fallos técnicos.
         * ignoreExceptions(...)	                No contabiliza un evento inexistente como fallo.
         *
         */

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(EventServiceUnavailableException.class)
                .ignoreExceptions(EventNotFoundException.class)
                .build();

        return CircuitBreaker.of("eventAvailability", config);
    }

    @Bean
    public Bulkhead eventAvailabilityBulkhead(){

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(5)
                .maxWaitDuration(Duration.ZERO)
                .build();

        return Bulkhead.of("eventAvailability", config);
    }

//      1/4 - Versión inicial: comunicación REST directa.
//            RestEventAvailabilityAdapter implementa EventAvailabilityPort.
//            No existe todavía ninguna política de resiliencia.
//    @Bean
//    public EventAvailabilityPort eventAvailabilityPort(
//    @Qualifier("eventRestClient") RestClient restClient) {
//        return new RestEventAvailabilityAdapter(restClient);
//    }

//      2/4 - Incorporación de Retry mediante el patrón Decorator.
//            RetryableEventAvailabilityAdapter envuelve al adaptador REST.
//            Reintenta fallos transitorios sin modificar la implementación HTTP.
//    @Bean
//    public EventAvailabilityPort eventAvailabilityPort(
//            @Qualifier("eventRestClient") RestClient restClient,
//            Retry eventAvailabilityRetry) {
//        EventAvailabilityPort restAdapter = new RestEventAvailabilityAdapter(restClient);
//        return new RetryableEventAvailabilityAdapter(restAdapter, eventAvailabilityRetry);
//    }

//      3/4 - Incorporación de Circuit Breaker mediante otro Decorator.
//            CircuitBreakerEventAvailabilityAdapter envuelve a RetryableEventAvailabilityAdapter.
//            Spring registra únicamente el decorador exterior como EventAvailabilityPort.

    //      4/4 - Incorporación de Bulk Head mediante otro Decorator.
//             BulkheadEventAvailabilityAdapter envuelve a CircuitBreakerEventAvailabilityAdapter.
//            Spring registra únicamente el decorador exterior como EventAvailabilityPort.
    @Bean
    public EventAvailabilityPort eventAvailabilityPort(
            @Qualifier("eventRestClient") RestClient restClient,
            Retry eventAvailabilityRetry,
            CircuitBreaker eventAvailabilityCircuitBreaker,
            Bulkhead eventAvailabilityBulkhead) {

        /**
         * Bulkhead es el decorador exterior.
         *
         * - Limita a 5 las operaciones concurrentes.
         * - Si no hay permiso, rechaza sin ejecutar Circuit Breaker, Retry ni HTTP.
         * - Circuit Breaker evalúa el resultado final de Retry.
         * - Retry puede realizar hasta 3 intentos HTTP.
         * - Un permiso de Bulkhead se mantiene durante toda la operación.
         */

        /**
         *  El Circuit Breaker es el decorador interior de Bulkhead.
         *      Esto significa que:
         *          - Si el circuito está OPEN, no se ejecuta ni Retry ni HTTP.
         *          - Si está CLOSED, Retry puede realizar hasta tres intentos.
         *          - El Circuit Breaker contabiliza el resultado final del conjunto de reintentos,
         *            no cada intento HTTP individual.
         *          - Si los tres intentos fallan, contabiliza un fallo.
         *          - Si el tercer intento funciona, contabiliza un éxito.
         */

        // 1. Comunicación HTTP
        EventAvailabilityPort restAdapter = new RestEventAvailabilityAdapter(restClient);

        // 2. Retry
        RetryableEventAvailabilityAdapter retryAdapter =
                new RetryableEventAvailabilityAdapter(restAdapter, eventAvailabilityRetry);

        // 3. Circuit Breaker
        CircuitBreakerEventAvailabilityAdapter cbAdapter = new CircuitBreakerEventAvailabilityAdapter(
                retryAdapter,
                eventAvailabilityCircuitBreaker
        );

        // 4. Bulk Head
        BulkheadEventAvailabilityAdapter bhAdapter = new BulkheadEventAvailabilityAdapter(
                cbAdapter,
                eventAvailabilityBulkhead
        );

        return bhAdapter;
    }

    @Bean
    public EventCapacityPort eventCapacityPort(
            @Qualifier("eventRestClient")RestClient restClient
    ){
        return new RestEventCapacityAdapter(restClient);
    }

    @Bean("eventRestClient")
    public RestClient eventRestClient(
            RestClient.Builder builder,
            @Value("${clients.event-service.base-url}") String baseUrl) {

        /** Timeout Configuration
         *
         * Coonection timeout:  2s.
         * Read Timeout:        3s.
         *
         * */

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }



}
