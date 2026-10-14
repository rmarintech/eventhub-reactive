package com.rubenmarin.bookingservice.infrastructure.adapter.in;

import com.rubenmarin.bookingservice.application.exception.BookingAlreadyExistsException;
import com.rubenmarin.bookingservice.application.exception.BookingNotFoundException;
import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.domain.model.*;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.BookingResponse;
import com.rubenmarin.bookingservice.infrastructure.web.generated.model.CreateBookingRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BookingUseCase bookingUseCase;

    @Test
    void shouldReturn201WhenBookingIsCreated() throws Exception {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);

        CreateBookingRequest createBookingRequest = new CreateBookingRequest(eventId.eventId(), userId.userId());

        Mockito.when(bookingUseCase.createBooking(eventId, userId)).thenReturn(booking);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingRequest))
                )

                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId").value(booking.getBookingId().bookingId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(userId.userId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.eventId").value(eventId.eventId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(BookingStatus.PENDING.name()));

        Mockito.verify(bookingUseCase).createBooking(eventId, userId);

    }

    @Test
    void shouldReturn400WhenEventIdIsMissing() throws Exception {

        UserId userId = new UserId(UUID.randomUUID());
        String json = """
                {
                    "userId": "%s"
                }
                """.formatted(userId.userId());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                        .value(Matchers.containsString("eventId")));

        Mockito.verify(bookingUseCase, Mockito.never()).createBooking(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldReturn200WhenBookingExists() throws Exception {

        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        Booking booking = new Booking(eventId, userId);
        UUID bookingId = booking.getBookingId().bookingId();

        Mockito.when(bookingUseCase.getBooking(new BookingId(bookingId))).thenReturn(booking);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/bookings/" + bookingId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId").value(booking.getBookingId().bookingId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.userId").value(userId.userId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.eventId").value(eventId.eventId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(BookingStatus.PENDING.name()));

        Mockito.verify(bookingUseCase).getBooking(new BookingId(bookingId));
    }


    @Test
    void shouldReturn404WhenBookingDoesNotExist() throws Exception {

        UUID bookingId = UUID.randomUUID();

        Mockito.when(bookingUseCase.getBooking(new BookingId(bookingId)))
                .thenThrow(new BookingNotFoundException(new BookingId(bookingId)));


        mockMvc.perform(MockMvcRequestBuilders.get("/api/bookings/" + bookingId))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content().contentType(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Booking Not Found"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("BOOKING_NOT_FOUND"));

    }


    @Test
    void shouldReturn409WhenBookingAlreadyExists() throws Exception {

        // Arrange
        EventId eventId = new EventId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        CreateBookingRequest request = new CreateBookingRequest(
                eventId.eventId(),
                userId.userId()
        );

        Mockito.when(bookingUseCase.createBooking(eventId, userId))
                .thenThrow(new BookingAlreadyExistsException(eventId, userId));

        // Act + Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Conflict"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code")
                        .value("BOOKING_ALREADY_EXISTS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                        .value(Matchers.containsString("Booking already exists")));

        Mockito.verify(bookingUseCase).createBooking(eventId, userId);
    }
}
