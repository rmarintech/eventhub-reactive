package com.rubenmarin.eventhub.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.CreateEventRequest;
import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.EventResponse;
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
    void shouldFindAllEvents() {

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

                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/events")
                /** el contentype que quiero que me conteste el server */
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectBodyList(EventResponse.class)


                .consumeWith(response -> {

                    List<EventResponse> events = response.getResponseBody();
                    Assertions.assertNotNull(events);
                    Assertions.assertEquals(1, events.size());
                    EventResponse event1st = events.getFirst();
                    Assertions.assertNotNull(event1st);
                    Assertions.assertNotNull(event1st.id());

                    Assertions.assertEquals("Reactive Java Workshop", event1st.name());
                    Assertions.assertEquals("Introduction to Project Reactor", event1st.description());
                    Assertions.assertNotNull(event1st.startDate());
                    Assertions.assertEquals(20, event1st.capacity());
                    Assertions.assertEquals(new BigDecimal("49.99"), event1st.price());
                    Assertions.assertEquals("EUR", event1st.currency());
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

}