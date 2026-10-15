
package com.rubenmarin.bookingservice.infrastructure.adapter.in.web;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingHttpIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @MockitoBean
    EventAvailabilityPort eventAvailabilityPort;


    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookingRepository repository;

    // POST /api/bookings -> 201 Created
    @Test
    void shouldCreateBookingThroughHttp() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        Mockito.when(eventAvailabilityPort.isAvailable(new EventId(eventId))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Comprobamos persistencia sin pasar por BookingService.
        Assertions.assertTrue(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));
    }

    // Primer POST -> 201; segundo POST -> 409 Conflict
//    Comprueba un duplicado secuencial, detectado normalmente por BookingService.existsActiveBooking().
//    No reproduce el conflicto concurrente que ya hemos probado a nivel de persistencia.
    @Test
    void shouldRejectDuplicateBookingThroughHttp() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        Mockito.when(eventAvailabilityPort.isAvailable(new EventId(eventId))).thenReturn(true);


        // Primera petición: reserva creada.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isCreated());

        // Segunda petición: mismo evento y usuario.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_EXISTS"));

        Mockito.verify(eventAvailabilityPort, Mockito.times(1))
                .isAvailable(new EventId(eventId));
    }

    @Test
    void shouldRejectBookingWhenEventIsNotAvailable() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Mockito.when(eventAvailabilityPort.isAvailable(new EventId(eventId))).thenReturn(false);

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EVENT_NOT_AVAILABLE"));

        Mockito.verify(eventAvailabilityPort).isAvailable(new EventId(eventId));

        Assertions.assertFalse(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));
    }

    @Test
    void shouldReturn503WhenEventServiceIsUnavailable() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Mockito.when(eventAvailabilityPort.isAvailable(new EventId(eventId)))
                .thenThrow(new EventServiceUnavailableException(
                        new RuntimeException("Connection refused")
                ));

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EVENT_SERVICE_UNAVAILABLE"));

        Assertions.assertFalse(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));
    }

    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Mockito.when(eventAvailabilityPort.isAvailable(new EventId(eventId)))
                .thenThrow(new EventNotFoundException(
                        new EventId(eventId),
                        new RuntimeException("404 from Event Service")
                ));

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

//        1. BookingService propaga correctamente la excepción del puerto.
//        2. GlobalExceptionHandler genera 404 EVENT_NOT_FOUND.
//        3. No se persiste ninguna reserva.

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));

        Assertions.assertFalse(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));
    }
}
