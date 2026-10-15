package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.domain.model.EventId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;


class RestEventAvailabilityAdapterTest {

    private MockRestServiceServer server;
    private RestEventAvailabilityAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8080");

        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new RestEventAvailabilityAdapter(builder.build());
    }

    @Test
    void shouldReturnTrueForPublishedEventWithAvailablePlaces() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andExpect(MockRestRequestMatchers.method(org.springframework.http.HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                            "id": "%s",
                            "status": "PUBLISHED",
                            "available": 10
                        }
                        """.formatted(uuid), MediaType.APPLICATION_JSON));

        boolean available = adapter.isAvailable(eventId);

        Assertions.assertTrue(available);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenEventHasNoAvailablePlaces() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                            "id": "%s",
                            "status": "PUBLISHED",
                            "available": 0
                        }
                        """.formatted(uuid), MediaType.APPLICATION_JSON));

        boolean available = adapter.isAvailable(eventId);

        Assertions.assertFalse(available);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenEventIsDraft() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                            "id": "%s",
                            "status": "DRAFT",
                            "available": 10
                        }
                        """.formatted(uuid), MediaType.APPLICATION_JSON));

        boolean available = adapter.isAvailable(eventId);

        Assertions.assertFalse(available);
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenEventIsCancelled() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                            "id": "%s",
                            "status": "CANCELLED",
                            "available": 10
                        }
                        """.formatted(uuid), MediaType.APPLICATION_JSON));

        boolean available = adapter.isAvailable(eventId);

        Assertions.assertFalse(available);
        server.verify();
    }

    @Test
    void shouldThrowExceptionWhenEventDoesNotExist() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(MockRestResponseCreators.withResourceNotFound());

        Assertions.assertThrows(EventNotFoundException.class, () -> adapter.isAvailable(eventId));
        server.verify();
    }

    @Test
    void shouldIgnoreAdditionalEventResponseFields() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                            "id": "%s",
                            "name": "Curso de escalada",
                            "description": "Curso de iniciación",
                            "startDate": "2026-11-20T09:00:00",
                            "capacity": 20,
                            "available": 10,
                            "price": 50.00,
                            "currency": "EUR",
                            "status": "PUBLISHED"
                        }
                        """.formatted(uuid), MediaType.APPLICATION_JSON));

        boolean available = adapter.isAvailable(eventId);

        Assertions.assertTrue(available);
        server.verify();
    }

    @Test
    void shouldRespondWithEventServiceUnavailable() {

        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

//         Simular una respuesta HTTP 500 con withServerError().
//         Verificar que el adaptador traduce el error a EventServiceUnavailableException.
//         Comprobar con server.verify() que se realizó la petición esperada.

        server.expect(MockRestRequestMatchers.requestTo("http://localhost:8080/events/" + uuid))
                .andRespond(MockRestResponseCreators.withServerError());

        Assertions.assertThrows(EventServiceUnavailableException.class, () -> adapter.isAvailable(eventId));
        server.verify();
    }

    @Test
    void shouldThrowEventServiceUnavailableWhenConnectionFails() {
// excepción de entrada/salida al realizar la petición HTTP.
        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        server.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/" + uuid
                ))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        EventServiceUnavailableException ex = Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.isAvailable(eventId)
        );

        Assertions.assertInstanceOf(ResourceAccessException.class, ex.getCause());

        server.verify();
    }
}
