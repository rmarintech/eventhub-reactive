package com.rubenmarin.eventhub.booking.infrastructure.config;

import com.rubenmarin.eventhub.booking.application.port.in.BookingQueryUseCase;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.port.out.BookingRepository;
import com.rubenmarin.eventhub.booking.application.service.BookingQueryService;
import com.rubenmarin.eventhub.booking.application.service.CreateBookingService;
import com.rubenmarin.eventhub.booking.infrastructure.transaction.TransactionalCreateBooking;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class BookingConfiguration {


    @Bean
    public CreateBookingUseCase createBookingUseCase(
            BookingRepository bookingRepository,
            ReserveEventPlacesUseCase reserveEventPlacesUseCase,
            TransactionalOperator transactionalOperator
    ) {
        CreateBookingService service =  new CreateBookingService(bookingRepository, reserveEventPlacesUseCase);

        return new TransactionalCreateBooking(
                service,
                transactionalOperator);
    }

    @Bean
    public BookingQueryUseCase bookingQueryUseCase(BookingRepository bookingRepository) {

        return new BookingQueryService(bookingRepository);
    }

}
