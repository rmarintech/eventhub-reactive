
package com.rubenmarin.bookingservice.infrastructure.adapter.in.web;

import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.domain.model.EventId;
import com.rubenmarin.bookingservice.domain.model.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingHttpIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

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

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Comprobamos persistencia sin pasar por BookingService.
        assertTrue(repository.existsActiveBooking(new EventId(eventId), new UserId(userId)));
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
        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());

        // Segunda petición: mismo evento y usuario.
        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_EXISTS"));
    }
}
