package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.event;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rubenmarin.aiassistantservice.application.exception.EventNotFoundException;
import com.rubenmarin.aiassistantservice.application.exception.EventServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class RestEventAvailabilityAdapterTest {

    /**
     * WebClient sends a GET request to /events/{id}.
     * The JSON response is deserialized correctly.
     * The adapter extracts availableCapacity.
     * No real Event Service or OpenAI connection is required.
     */

    private WireMockServer wireMockServer;
    private RestEventAvailabilityAdapter restEventAvailabilityAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());

        wireMockServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(wireMockServer.baseUrl())
                .build();

        restEventAvailabilityAdapter = new RestEventAvailabilityAdapter(webClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldReturnAvailableSeats() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        // Configure WireMock to return availableCapacity = 25
        wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/events/" + eventId))
                        .willReturn(
                                WireMock.okJson("""
                                        {
                                            "availableCapacity": 25,
                                            "totalCapacity": 100,
                                            "name": "Java Conference"
                                        }
                                        """)
                        )
        );

        // WHEN
        Mono<Integer> result = restEventAvailabilityAdapter.getAvailableSeats(eventId);

        // THEN
        StepVerifier.create(result)
                .expectNext(25)
                .verifyComplete();

        wireMockServer.verify(
                1,
                WireMock.getRequestedFor(
                        WireMock.urlEqualTo("/events/" + eventId)
                ));
    }

    @Test
    void shouldHandleEventNotFound404() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/events/" + eventId))
                        .willReturn(WireMock.notFound())
        );

        // WHEN
        Mono<Integer> result = restEventAvailabilityAdapter.getAvailableSeats(eventId);

        // THEN
        StepVerifier.create(result)
                //.expectError(WebClientResponseException.NotFound.class)
                .expectError(EventNotFoundException.class)
                .verify();
    }

    @Test
    void shouldHandleServiceUnavailable503() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/events/" + eventId))
                        .willReturn(WireMock.serviceUnavailable())
        );

        // WHEN
        Mono<Integer> result = restEventAvailabilityAdapter.getAvailableSeats(eventId);

        // THEN
        StepVerifier.create(result)
//                .expectError(WebClientResponseException.ServiceUnavailable.class)
                .expectError(EventServiceUnavailableException.class)
                .verify();
    }

    @Test
    void shouldHandleConnectionFailure() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/events/" + eventId))
                        .willReturn(WireMock.serviceUnavailable())
        );

        wireMockServer.stop();

        // WHEN
        Mono<Integer> result = restEventAvailabilityAdapter.getAvailableSeats(eventId);

        // THEN
        StepVerifier.create(result)
                .expectError(EventServiceUnavailableException.class)
                .verify();
    }
}
