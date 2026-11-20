package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final AskAiUseCase askAiUseCase;

    public AiController(AskAiUseCase askAiUseCase) {
        this.askAiUseCase = askAiUseCase;
    }

    /*public AiController(AskAiUseCase askAiUseCase) {
        this.askAiUseCase = askAiUseCase;
    }*/

    @PostMapping("/ask")
    public Mono<String> post(@RequestBody AiRequest request) {
        return Mono.fromCallable(
                () -> {
                    //log.info("Current span: {}", tracer.currentSpan());
                    log.info("Executing on: {}", Thread.currentThread().getName());
                    return askAiUseCase.ask(request.question());
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

//    @PostMapping(
//            value = "/ask/stream",
//            produces = MediaType.TEXT_EVENT_STREAM_VALUE
//    )
//    public Flux<String> stream(@RequestBody AiRequest request) {

    /// /        $body = @{ question = "Hello" } | ConvertTo-Json
    /// /
    /// /        Invoke-WebRequest `
    /// /        -Uri "http://localhost:8083/api/v1/ai/ask/stream" `
    /// /        -Method POST `
    /// /        -ContentType "application/json" `
    /// /        -Headers @{ Accept = "text/event-stream" } `
    /// /        -Body $body
//
//        return askAiUseCase.stream(request.question());
//    }
    @PostMapping(
            value = "/ask/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> stream(@RequestBody AiRequest request) {

        return askAiUseCase.stream(request.question())
                .switchOnFirst((signal, flux) -> {
                    // Si falla antes del primer evento, mantenemos el HTTP 503.
                    if (signal.isOnError()) {
                        return Flux.error(signal.getThrowable());
                    }
                    // Convertimos los fragmentos de texto en eventos SSE.
                    return flux.map(text -> ServerSentEvent.<String>builder()
                                    .event("message")
                                    .data(text)
                                    .build())
                            // Si falla después de empezar, enviamos un evento de error.
                            .onErrorResume(AiProviderException.class, ex ->
                                    Flux.just(ServerSentEvent.<String>builder()
                                            .event("error")
                                            .data("AI service temporarily unavailable")
                                            .build())
                            );
                });
    }
}
