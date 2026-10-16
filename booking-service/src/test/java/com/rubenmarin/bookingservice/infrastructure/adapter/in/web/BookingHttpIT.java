
package com.rubenmarin.bookingservice.infrastructure.adapter.in.web;

import com.rubenmarin.bookingservice.application.exception.EventNotAvailableException;
import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
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
    EventCapacityPort eventCapacityPort;

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

        Mockito.verify(eventCapacityPort, Mockito.times(1))
                .reservePlaces(new EventId(eventId), 1);

        Mockito.verify(eventCapacityPort, Mockito.never())
                .releasePlaces(new EventId(eventId), 1);

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

        //¿Por qué dos reservas y una liberación?
        //  - Primera petición: reserva una plaza y crea el Booking (201).
        //  - Segunda petición: reserva una plaza, detecta el Booking duplicado (409)
        //      y libera la plaza mediante compensación.

        Mockito.verify(eventCapacityPort, Mockito.times(2))
                .reservePlaces(new EventId(eventId), 1);

        Mockito.verify(eventCapacityPort, Mockito.times(1))
                .releasePlaces(new EventId(eventId), 1);
    }

    @Test
    void shouldRejectBookingWhenEventIsNotAvailable() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        Mockito.doThrow(new EventNotAvailableException(new EventId(eventId)))
                .when(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EVENT_NOT_AVAILABLE"));


        Assertions.assertFalse(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));

        // Aquí no hay compensación porque la reserva remota nunca ha llegado a completarse correctamente.
        //Conserva tus comprobaciones de HTTP 409, código EVENT_NOT_AVAILABLE y ausencia de persistencia.
        Mockito.verify(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);

        Mockito.verify(eventCapacityPort, Mockito.never())
                .releasePlaces(new EventId(eventId), 1);

    }

    @Test
    void shouldReturn503WhenEventServiceIsUnavailable() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        Mockito.doThrow(new EventServiceUnavailableException(
                        new RuntimeException("Connection refused")))
                .when(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EVENT_SERVICE_UNAVAILABLE"));

        Assertions.assertFalse(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));

        // Conserva las comprobaciones de HTTP 503, código EVENT_SERVICE_UNAVAILABLE y ausencia de persistencia.
        Mockito.verify(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);
    }

    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String body = """
                {
                    "eventId": "%s",
                    "userId": "%s"
                }
                """.formatted(eventId, userId);

        Mockito.doThrow(new EventNotFoundException(
                        new EventId(eventId),
                        new RuntimeException("404 from Event Service")))
                .when(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);

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

        //Conserva las comprobaciones de HTTP 404, código EVENT_NOT_FOUND y ausencia de persistencia.
        Mockito.verify(eventCapacityPort)
                .reservePlaces(new EventId(eventId), 1);

    }
}
