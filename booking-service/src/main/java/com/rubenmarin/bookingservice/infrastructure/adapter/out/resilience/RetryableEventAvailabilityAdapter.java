package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.retry.Retry;

public class RetryableEventAvailabilityAdapter implements EventAvailabilityPort {

    private final EventAvailabilityPort delegate;
    private final Retry retry;

    public RetryableEventAvailabilityAdapter(EventAvailabilityPort delegate, Retry retry) {
        this.delegate = delegate;
        this.retry = retry;
    }

    @Override
    public boolean isAvailable(EventId eventId) {
        return Retry.decorateSupplier(retry, () -> delegate.isAvailable(eventId)).get();
    }
}
