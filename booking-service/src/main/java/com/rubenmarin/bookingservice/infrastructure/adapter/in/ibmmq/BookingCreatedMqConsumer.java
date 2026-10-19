package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class BookingCreatedMqConsumer {

    private static final Logger _log = LoggerFactory.getLogger(BookingCreatedMqConsumer.class);


    @JmsListener(destination = "BOOKING.CREATED.Q")
    public void consume(String payload){

        _log.info("******************************************");
        _log.info("Received Booking Created Event: {}", payload);
        throw new RuntimeException("Simulated processing failure");
    }
}
