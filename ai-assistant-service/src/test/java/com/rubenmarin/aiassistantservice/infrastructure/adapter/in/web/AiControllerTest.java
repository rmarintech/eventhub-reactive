package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

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
}