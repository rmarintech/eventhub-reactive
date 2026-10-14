package com.rubenmarin.bookingservice.infrastructure.adapter.in.mapper;

import com.rubenmarin.bookingservice.domain.model.Booking;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.BookingResponse;

public class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {

        BookingResponse response = new BookingResponse();

        response.setBookingId(booking.getBookingId().bookingId());
        response.setEventId(booking.getEventId().eventId());
        response.setUserId(booking.getUserId().userId());

        response.setStatus(
                BookingResponse.StatusEnum.fromValue(
                        booking.getStatus().name()
                )
        );

        return response;
    }


}