package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai;

import com.openai.errors.RateLimitException;
import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SpringAiAdapterTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private GetAvailableSeatsUseCase getAvailableSeatsUseCase;
    @Mock
    private EventTools eventTools;

    private SpringAiAdapter springAiAdapter;

    private ChatClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = ChatClient.builder(chatModel);

//        Mockito.when(chatModel.getOptions())
//                .thenReturn(OpenAiChatOptions.builder()
//                        .model("gpt-5-nano")
//                        .build());

        Mockito.when(chatModel.getOptions())
                .thenReturn(ToolCallingChatOptions.builder().build());


    }

    @Test
    void shouldStreamResponse() {

        springAiAdapter = new SpringAiAdapter(builder, eventTools);

        ChatResponse firstResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("Hello "))));

        ChatResponse secondResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("World!"))));

        Mockito.when(chatModel.stream(Mockito.any(Prompt.class)))
                .thenReturn(Flux.just(firstResponse, secondResponse));

        // TODO: Verify the emitted elements

        StepVerifier.create(springAiAdapter.streamResponse("Say Hello World!"))
                .expectNext("Hello ")
                .expectNext("World!")
                .verifyComplete();

        Mockito.verify(chatModel, Mockito.times(1)).stream(Mockito.any(Prompt.class));
    }

    @Test
    void shouldHandleRateLimitExceptionDuringStreaming() {

        springAiAdapter = new SpringAiAdapter(builder, eventTools);

        RateLimitException rateLimitExc = Mockito.mock(RateLimitException.class);

        Mockito.when(chatModel.stream(Mockito.any(Prompt.class)))
                .thenReturn(Flux.error(rateLimitExc));

        StepVerifier.create(springAiAdapter.streamResponse("Say Hello World!"))
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(AiProviderException.class, error);
                    Assertions.assertSame(rateLimitExc, error.getCause());
                })
                .verify();
    }


    @Test
    void shouldGenerateResponse() {

        // Creamos un spy de EventTools para ejecutar sus métodos reales
        // y poder verificar posteriormente que han sido invocados.
        // GetAvailableSeatsUseCase es un mock, por lo que no accedemos a EventHub.
        EventTools spyEventTools = Mockito.spy(new EventTools(getAvailableSeatsUseCase));

        // Construimos el adaptador real con el ChatModel simulado
        // y las herramientas que queremos probar.
        springAiAdapter = new SpringAiAdapter(builder, spyEventTools);

        // GIVEN: preparamos la pregunta del usuario con un UUID aleatorio.
        UUID eventId = UUID.randomUUID();
        String prompt = "How many seats are available for event " + eventId.toString() + "?";

        // PASO 1: simulamos que el modelo decide utilizar una herramienta.
        //
        // ToolCall representa la petición que genera el LLM para ejecutar
        // una función, indicando su nombre y los argumentos en formato JSON.
        //
        // Todavía no se ejecuta ninguna herramienta.
        AssistantMessage.ToolCall toolCall =
                new AssistantMessage.ToolCall(
                        "call-" + eventId.toString(),
                        "function",
                        "getAvailableSeats",
                        "{\"eventId\":\"" + eventId.toString() + "\"}"
                );

        // PASO 2: creamos el mensaje del asistente que contiene
        // la solicitud de ejecución de la herramienta.
        // En este momento el modelo no responde al usuario,
        // sino que solicita ejecutar getAvailableSeats.
        AssistantMessage assistantMessage =
                AssistantMessage
                        .builder()
                        .toolCalls(List.of(toolCall))
                        .build();

        // Generation representa una generación del modelo.
        // ChatResponse contiene la respuesta completa del modelo.
        //
        // Esta primera respuesta solicita ejecutar la herramienta.
        ChatResponse toolCallResponse = new ChatResponse(List.of(new Generation(assistantMessage)));

        // PASO 3: preparamos la respuesta final que devolverá el modelo
        // después de recibir el resultado de la herramienta.
        String llmMessage = "Event " + eventId.toString() + " has 25 available seats.";

        AssistantMessage finalMessage = new AssistantMessage(llmMessage);

        ChatResponse finalResponse = new ChatResponse(List.of(new Generation(finalMessage)));

        // PASO 4: simulamos la respuesta del caso de uso.
        // Cuando EventTools consulte las plazas disponibles, el caso de uso devolverá 25 sin llamar al servicio real.
        Mockito.when(getAvailableSeatsUseCase.getAvailableSeats(eventId)).thenReturn(Mono.just(25));

        // PASO 5: configuramos las dos respuestas consecutivas del modelo.
        //
        // Primera llamada: el LLM solicita ejecutar getAvailableSeats.
        // Segunda llamada: el LLM devuelve su respuesta final.
        //
        // Entre ambas llamadas, Spring AI ejecutará la herramienta y enviará su resultado de nuevo al modelo.
        Mockito.when(chatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        // WHEN: ejecutamos el adaptador real.
        //
        // ChatClient gestionará automáticamente el ciclo de tool calling:
        // petición al modelo -> ejecución de herramienta -> nueva petición al modelo -> respuesta final.
        String response = springAiAdapter.generateResponse(prompt);

        // THEN: comprobamos que recibimos la respuesta final esperada.
        Assertions.assertEquals(llmMessage, response);

        // Capturamos los prompts enviados al modelo.
        // Esperamos dos llamadas: una inicial y otra después del tool calling.
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);

        Mockito.verify(chatModel, Mockito.times(2)).call(promptCaptor.capture());

        List<Prompt> capturedPrompts = promptCaptor.getAllValues();

        // PRIMER PROMPT:
        // verificamos que contiene opciones compatibles con tool calling.
        Prompt firstPrompt = capturedPrompts.get(0);

        Assertions.assertTrue(firstPrompt.getOptions() instanceof ToolCallingChatOptions);

        ToolCallingChatOptions options =
                (ToolCallingChatOptions) firstPrompt.getOptions();

        // Verificamos que la herramienta getAvailableSeats está registrada
        // y disponible para que el modelo pueda solicitar su ejecución.
        Assertions.assertTrue(
                options.getToolCallbacks().stream()
                        .anyMatch(callback ->
                                callback.getToolDefinition()
                                        .name()
                                        .equals("getAvailableSeats"))
        );

        // SEGUNDO PROMPT:
        // después de ejecutar la herramienta, Spring AI vuelve a llamar al modelo incluyendo el resultado obtenido.
        Prompt secondPrompt = capturedPrompts.get(1);

        // Recuperamos la pregunta original del usuario.
        Message userMessage = secondPrompt.getUserMessage();

        // Recuperamos el mensaje con el resultado de la herramienta.
        ToolResponseMessage toolMessage = (ToolResponseMessage) secondPrompt.getLastUserOrToolResponseMessage();

        // Extraemos el resultado enviado al LLM.
        String responseData = toolMessage.getResponses().getFirst().responseData();

        // Comprobamos que se conserva la pregunta original.
        Assertions.assertEquals(prompt, userMessage.getText());

        // Comprobamos que Spring AI envía al modelo el resultado "25".
        Assertions.assertEquals("25", responseData);

        // Verificamos que Spring AI ejecutó realmente la herramienta una vez.
        Mockito.verify(spyEventTools, Mockito.times(1))
                .getAvailableSeats(eventId.toString());

        // Verificamos que la herramienta delegó en el caso de uso con el UUID correcto
        // y que este fue invocado una sola vez.
        Mockito.verify(getAvailableSeatsUseCase, Mockito.times(1))
                .getAvailableSeats(eventId);
    }


    @Test
    void shouldThrowAiProviderExceptionWhenRateLimitExceeded() {
        RateLimitException rateLimitExc = Mockito.mock(RateLimitException.class);

        Mockito.when(chatModel.call(Mockito.any(Prompt.class))).thenThrow(rateLimitExc);

        springAiAdapter = new SpringAiAdapter(builder, eventTools);

        AiProviderException aiProviderExc = Assertions.assertThrows(
                AiProviderException.class,
                () -> springAiAdapter.generateResponse("How many seats are available?")
        );

        Assertions.assertSame(rateLimitExc, aiProviderExc.getCause());

    }

}
