package com.rubenmarin.eventhub.booking.infrastructure.transaction;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.application.service.CreateBookingService;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class TransactionalCreateBooking implements CreateBookingUseCase {
    private final TransactionalOperator transactionalOperator;

    private final CreateBookingUseCase delegate;

    public TransactionalCreateBooking (CreateBookingService delegate, TransactionalOperator transactionalOperator){
        this.delegate = delegate;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<Booking> createBooking(CreateBookingCommand command) {

        Mono<Booking> operation = delegate.createBooking(command);

        return transactionalOperator.transactional(operation);
    }
}
