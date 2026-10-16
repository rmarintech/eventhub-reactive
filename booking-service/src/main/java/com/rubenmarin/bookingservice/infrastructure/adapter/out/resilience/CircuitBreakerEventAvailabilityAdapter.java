package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

public class CircuitBreakerEventAvailabilityAdapter implements EventAvailabilityPort {

    private final EventAvailabilityPort delegate;
    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerEventAvailabilityAdapter(
            EventAvailabilityPort delegate,
            CircuitBreaker circuitBreaker) {

        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public boolean isAvailable(EventId eventId) {
        try {
        return CircuitBreaker.decorateSupplier(circuitBreaker, () -> delegate.isAvailable(eventId)).get();
        } catch (CallNotPermittedException ex) {

            // Circuito abierto (CallNotPermittedException)
            // lo gestionamos como EventServiceUnavailableException,  HTTP 503.
            throw new EventServiceUnavailableException(ex);
        }
    }
}
