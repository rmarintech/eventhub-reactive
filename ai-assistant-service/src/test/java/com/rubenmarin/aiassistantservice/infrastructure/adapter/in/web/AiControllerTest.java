package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

@ActiveProfiles("test")
@WebFluxTest(AiController.class)
@Import(GlobalExceptionHandler.class)
class AiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AskAiUseCase askAiUseCase;

    @Test
    void shouldReturn503WhenAiProviderFails() {

        // Arrange
        Mockito.when(askAiUseCase.ask(Mockito.anyString()))
                .thenThrow(new AiProviderException(
                        "AI service temporarily unavailable",
                        new RuntimeException("OpenAI credits exhausted")
                ));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/ai/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("question", "Hello"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("AI_SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("AI service temporarily unavailable");
    }

    @Test
    void shouldStreamResponseAsSse() {

        // GIVEN: Simulamos una respuesta de IA en dos fragmentos.
        Mockito.when(askAiUseCase.stream("Say Hello World!"))
                .thenReturn(Flux.just("Hello ", "World!"));

        // WHEN: Ejecutamos el endpoint SSE.
        FluxExchangeResult<ServerSentEvent<String>> result =
                webTestClient.post()
                        .uri("/api/v1/ai/ask/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(Map.of("question", "Say Hello World!"))
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader()
                        .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                        .returnResult(
                                new ParameterizedTypeReference<ServerSentEvent<String>>() {
                                }
                        );

        // THEN: Verificamos el tipo y contenido de cada evento SSE.
        StepVerifier.create(result.getResponseBody())
                .assertNext(event -> {
                    Assertions.assertEquals("message", event.event());
                    Assertions.assertEquals("Hello ", event.data());
                })
                .assertNext(event -> {
                    Assertions.assertEquals("message", event.event());
                    Assertions.assertEquals("World!", event.data());
                })
                .verifyComplete();

        // Verificamos que el controlador delegó en el caso de uso.
        Mockito.verify(askAiUseCase, Mockito.times(1)).stream("Say Hello World!");

    }

    @Test
    void shouldReturn503WhenStreamingFailsBeforeFirstEvent() {

        // GIVEN
        AiProviderException exception = new AiProviderException(
                "AI service temporarily unavailable",
                new RuntimeException("OpenAI credits exhausted")
        );

        Mockito.when(askAiUseCase.stream("Hello")).thenReturn(Flux.error(exception));

        // WHEN & THEN
        webTestClient.post()
                .uri("/api/v1/ai/ask/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(Map.of("question", "Hello"))
                .exchange()
                .expectStatus().isEqualTo(503);

        Mockito.verify(askAiUseCase).stream("Hello");
    }

    @Test
    void shouldHandleErrorAfterFirstSseEvent() {

        // GIVEN
        AiProviderException exception = new AiProviderException(
                "AI service temporarily unavailable",
                new RuntimeException("OpenAI credits exhausted")
        );

//        Mockito.when(askAiUseCase.stream("Hello"))
//                .thenReturn(Flux.just("Hello ")
//                        .concatWith(Flux.error(exception)));

        // Creamos un Sink para controlar cuándo se emite el error.
        Sinks.Many<String> sink = Sinks.many()
                .unicast()
                .onBackpressureBuffer();

        // Primero enviamos "Hello ".
        // Después, el Flux queda esperando eventos del Sink.
        Mockito.when(askAiUseCase.stream("Hello"))
                .thenReturn(
                        Flux.concat(
                                Mono.just("Hello "),
                                sink.asFlux()
                        )
                );

        // WHEN
        FluxExchangeResult
                <ServerSentEvent<String>> result = webTestClient.post()
                .uri("/api/v1/ai/ask/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(Map.of("question", "Hello"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                });

        // THEN
        StepVerifier.create(result.getResponseBody())
                .assertNext(event -> {
                    Assertions.assertEquals("message", event.event());
                    Assertions.assertEquals("Hello ", event.data());
                })
                .then(() -> {
                    // Emitimos el error solo después de recibir el primer evento.
                    Assertions.assertEquals(Sinks.EmitResult.OK, sink.tryEmitError(exception));
                })
                .assertNext(event -> {
                    Assertions.assertEquals("error", event.event());
                    Assertions.assertEquals(
                            "AI service temporarily unavailable",
                            event.data()
                    );
                })
                .verifyComplete();
    }
}