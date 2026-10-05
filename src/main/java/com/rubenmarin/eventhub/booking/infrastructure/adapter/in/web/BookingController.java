package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingCommand;
import com.rubenmarin.eventhub.booking.application.port.in.CreateBookingUseCase;
import com.rubenmarin.eventhub.booking.domain.model.Booking;
import com.rubenmarin.eventhub.booking.domain.model.CustomerId;
import com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web.exception.InvalidIdException;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {


    private final CreateBookingUseCase createBookingUseCase;

    public BookingController(CreateBookingUseCase createBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest rq) {

        Mono<Booking> booking = createBookingUseCase.createBooking(toCommand(rq));
        Mono<BookingResponse> response = booking.map(value -> toResponse(value));
        return response;
    }

    private CreateBookingCommand toCommand(CreateBookingRequest rq) {
        return new CreateBookingCommand(
                new CustomerId(parseId(rq.customerId())),
                new EventId(parseId(rq.eventId())),
                rq.places()
        );
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId().value(),
                booking.getCustomerId().value(),
                booking.getEventId().value(),
                booking.getPlaces(),
                booking.getStatus().name()

        );
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("Not a valid uuid: " + id);
        }
    }

}
