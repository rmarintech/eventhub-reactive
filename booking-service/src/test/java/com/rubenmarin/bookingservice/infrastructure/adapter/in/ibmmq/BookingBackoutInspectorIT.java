package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingBackoutInspectorIT {

    @Autowired
    BookingBackoutInspector bookingBackoutInspector;

    @Test
    void shouldInspectBookingBackout() {
        bookingBackoutInspector.inspect();
    }
}
