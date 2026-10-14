package com.rubenmarin.bookingservice.infrastructure.adapter.in;

import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.domain.model.BookingId;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;
import com.rubenmarin.bookingservice.infrastructure.adapter.in.mapper.BookingMapper;
import com.rubenmarin.bookingservice.infrastructure.web.generated.api.BookingApi;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.BookingResponse;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.CreateBookingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class BookingController implements BookingApi {

    private final BookingUseCase bookingUseCase;

    public BookingController(BookingUseCase bookingUseCase) {
        this.bookingUseCase = bookingUseCase;
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(CreateBookingRequest createBookingRequest) {

        EventId eventId = new EventId(createBookingRequest.getEventId());
        UserId userId = new UserId(createBookingRequest.getUserId());

        Booking booking = bookingUseCase.createBooking(eventId, userId);

        BookingResponse bookingResponse = BookingMapper.toResponse(booking);

        return new ResponseEntity<>(bookingResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<BookingResponse> getBooking(UUID id) {

        Booking booking = bookingUseCase.getBooking(new BookingId(id));

        BookingResponse bookingResponse = BookingMapper.toResponse(booking);

        return new ResponseEntity<>(bookingResponse, HttpStatus.OK);
    }


}
