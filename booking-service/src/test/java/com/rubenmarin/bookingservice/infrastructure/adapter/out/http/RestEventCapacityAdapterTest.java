package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import com.rubenmarin.bookingservice.application.exception.EventNotAvailableException;
import com.rubenmarin.bookingservice.application.exception.EventNotFoundException;
import com.rubenmarin.bookingservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.bookingservice.domain.model.EventId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

class RestEventCapacityAdapterTest {

    private MockRestServiceServer mockServer;
    private RestEventCapacityAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8080");

        mockServer = MockRestServiceServer.bindTo(builder).build();
        adapter = new RestEventCapacityAdapter(builder.build());
    }

    @Test
    void shouldThrowEventNotAvailableWhenReservationReturns409() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/reserve/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT));

        Assertions.assertThrows(
                EventNotAvailableException.class,
                () -> adapter.reservePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowEventNotFoundWhenReservationReturns404() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/reserve/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(
                EventNotFoundException.class,
                () -> adapter.reservePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowEventServiceUnavailableWhenReservationReturns500() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/reserve/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.reservePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldReservePlacesSuccessfullyWhenEventServiceReturns200() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/reserve/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));

        Assertions.assertDoesNotThrow(
                () -> adapter.reservePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldReleasePlacesSuccessfullyWhenEventServiceReturns200() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/release/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess());

        Assertions.assertDoesNotThrow(
                () -> adapter.releasePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowEventNotFoundWhenReleaseReturns404() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/release/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(
                EventNotFoundException.class,
                () -> adapter.releasePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldPropagateConflictWhenReleaseReturns409() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/release/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CONFLICT));

        Assertions.assertThrows(
                HttpClientErrorException.Conflict.class,
                () -> adapter.releasePlaces(eventId, 1)
        );

        mockServer.verify();
    }

    @Test
    void shouldThrowEventServiceUnavailableWhenReleaseReturns500() {

        EventId eventId = new EventId(UUID.randomUUID());

        mockServer.expect(MockRestRequestMatchers.requestTo(
                        "http://localhost:8080/events/"
                                + eventId.eventId()
                                + "/release/1/places"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Assertions.assertThrows(
                EventServiceUnavailableException.class,
                () -> adapter.releasePlaces(eventId, 1)
        );

        mockServer.verify();
    }


}