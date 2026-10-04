package com.rubenmarin.eventhub.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.CreateEventRequest;
import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.EventResponse;
import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.exception.ValidationErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureWebTestClient
class EventControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateEvent() {

        CreateEventRequest request = new CreateEventRequest(
                "Reactive Java Workshop",
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                20,
                new BigDecimal("49.99"),
                "EUR"
        );

        webTestClient.post()
                .uri("/events")
                /** el contentype que consume (request)*/
                .contentType(MediaType.APPLICATION_JSON)
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()

                //.expectStatus().is2xxSuccessful()
                //.expectStatus().isOk()
                /** el contentype que produce (Response)*/
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isCreated()
                .expectBody(EventResponse.class)
                .consumeWith(response -> {

                    EventResponse body = response.getResponseBody();
                    Assertions.assertNotNull(body);
                    Assertions.assertNotNull(body.id());

                    Assertions.assertEquals("Reactive Java Workshop", body.name());
                    Assertions.assertEquals("Introduction to Project Reactor", body.description());
                    Assertions.assertNotNull(body.startDate());
                    Assertions.assertEquals(20, body.capacity());
                    Assertions.assertEquals(new BigDecimal("49.99"), body.price());
                    Assertions.assertEquals("EUR", body.currency());
                });
    }

    @Test
    void shouldNotCreateEvent() {

        CreateEventRequest request = new CreateEventRequest(
                "",
                "",
                null,
                0,
                new BigDecimal("-10"),
                ""
        );

        webTestClient.post()
                .uri("/events")
                /** el contentype que consume (request)*/
                .contentType(MediaType.APPLICATION_JSON)
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()

                /** el contentype que produce (Response)*/
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectStatus().isBadRequest()

        .expectBody(ValidationErrorResponse.class)
                .consumeWith(postResponse -> {System.out.println(postResponse.getResponseBody());});
                ;
    }


    @Test
    void shouldFindAllEvents() {

        CreateEventRequest createEventRq = new CreateEventRequest(
                "Reactive Java Workshop",
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                20,
                new BigDecimal("49.99"),
                "EUR"
        );

        webTestClient.post()
                .uri("/events")
                /** el contentype que consume (request)*/
                .contentType(MediaType.APPLICATION_JSON)
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createEventRq)

                .exchange()

                .expectStatus().isCreated()
                .expectBody(EventResponse.class)
                .consumeWith(postResponse -> {

                    webTestClient.get()
                            .uri("/events")
                            /** el contentype que quiero que me conteste el server */
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()

                            .expectStatus().isOk()
                            .expectBodyList(EventResponse.class)
                            .consumeWith(getResponse -> {

                                List<EventResponse> events = getResponse.getResponseBody();

                                EventResponse eventPosted = postResponse.getResponseBody();

                                Assertions.assertNotNull(events);

                                EventResponse eventFound = events.stream().filter(value -> value.id().equals(eventPosted.id())).findFirst().orElseThrow();

                                Assertions.assertNotNull(eventFound.id());
                                Assertions.assertEquals(eventPosted.id(),eventFound.id());
                                Assertions.assertEquals("Reactive Java Workshop", eventFound.name());
                                Assertions.assertEquals("Introduction to Project Reactor", eventFound.description());
                                Assertions.assertNotNull(eventFound.startDate());
                                Assertions.assertEquals(20, eventFound.capacity());
                                Assertions.assertEquals(new BigDecimal("49.99"), eventFound.price());
                                Assertions.assertEquals("EUR", eventFound.currency());
                            });
                });
    }

    @Test
    void shouldFindEvent() {

        CreateEventRequest request = new CreateEventRequest(
                "Reactive Java Workshop",
                "Introduction to Project Reactor",
                LocalDateTime.now().plusDays(30),
                20,
                new BigDecimal("49.99"),
                "EUR"
        );

        webTestClient.post()
                .uri("/events")
                /** el contentype que consume (request)*/
                .contentType(MediaType.APPLICATION_JSON)
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()

                .expectStatus().isCreated()

                .expectBody(EventResponse.class)
                .consumeWith(postResponse -> {

                    Assertions.assertNotNull(postResponse.getResponseBody());
                    String postResponseEventId = postResponse.getResponseBody().id().toString();

                    webTestClient.get()
                            .uri("/events/" + postResponseEventId)

                            /** el contentype que quiero que me conteste el server */
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()

                            .expectStatus().isOk()
                            .expectBody(EventResponse.class)
                            .consumeWith(getResponse -> {

                                EventResponse event = getResponse.getResponseBody();
                                Assertions.assertNotNull(event);
                                Assertions.assertEquals(postResponseEventId, event.id().toString());

                                Assertions.assertEquals("Reactive Java Workshop", event.name());
                                Assertions.assertEquals("Introduction to Project Reactor", event.description());
                                Assertions.assertNotNull(event.startDate());
                                Assertions.assertEquals(20, event.capacity());
                                Assertions.assertEquals(new BigDecimal("49.99"), event.price());
                                Assertions.assertEquals("EUR", event.currency());
                            });
                });


    }


    @Test
    void shouldNotFindEvent() {
        webTestClient.get()
                .uri("/events/" + UUID.randomUUID())
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnBadRequestWhenEventIdIsInvalid() {
        webTestClient.get()
                .uri("/events/" + "Not_AN_UUID")
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isBadRequest();
    }

}