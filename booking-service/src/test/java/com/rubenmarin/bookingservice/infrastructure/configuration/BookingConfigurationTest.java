package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BookingConfigurationTest {

    @Autowired
    BookingUseCase bookingUseCase;

    @Autowired
    BookingRepository bookingRepository;

    @Test
    void shouldLoadBookingBeans() {

        assertNotNull(bookingUseCase);
        assertNotNull(bookingRepository);
    }
}