package com.rubenmarin.bookingservice.infrastructure.adapter.out.resilience;

import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;

public class BulkheadEventAvailabilityAdapter implements EventAvailabilityPort {

    private final EventAvailabilityPort delegate;
    private final Bulkhead bulkhead;

    public BulkheadEventAvailabilityAdapter(
            EventAvailabilityPort delegate,
            Bulkhead bulkhead) {

        this.delegate = delegate;
        this.bulkhead = bulkhead;
    }

    @Override
    public boolean isAvailable(EventId eventId) {
        try {
            return Bulkhead.decorateSupplier(bulkhead, () -> delegate.isAvailable(eventId)).get();
        } catch (BulkheadFullException ex) {
            throw new EventServiceUnavailableException(ex);
        }
    }
}
