package com.rubenmarin.bookingservice.application.port.in;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;

public interface ProcessBookingCreatedUseCase {

    public void process(BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent);
}
