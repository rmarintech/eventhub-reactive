package com.rubenmarin.eventhub.booking.infrastructure.config;

import com.rubenmarin.eventhub.booking.application.port.in.BookingQueryUseCase;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.application.service.BookingQueryService;
import com.rubenmarin.eventhub.booking.application.service.CreateBookingService;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import com.rubenmarin.eventhub.event.application.service.ReserveEventPlacesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingConfiguration {


    @Bean
    public CreateBookingUseCase createBookingUseCase(
            BookingRepository bookingRepository,
            ReserveEventPlacesUseCase reserveEventPlacesUseCase
    ) {

        return new CreateBookingService(
                bookingRepository,
                reserveEventPlacesUseCase);
    }

    @Bean
    public BookingQueryUseCase bookingQueryUseCase(BookingRepository bookingRepository) {

        return new BookingQueryService(bookingRepository);
    }

}
