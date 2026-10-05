package com.rubenmarin.eventhub.booking.infrastructure.config;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
 class BookingConfigurationTest {

    @Autowired
    private CreateBookingUseCase createBookingUseCase;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void shouldWireBookingDependencies() {

        Assertions.assertNotNull(createBookingUseCase);
        Assertions.assertNotNull(bookingRepository);
    }
}
