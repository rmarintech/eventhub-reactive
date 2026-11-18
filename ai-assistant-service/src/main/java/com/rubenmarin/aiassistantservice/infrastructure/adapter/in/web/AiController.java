package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import org.springframework.http.MediaType;
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

    private final AskAiUseCase askAiUseCase;

    public AiController(AskAiUseCase askAiUseCase) {
        this.askAiUseCase = askAiUseCase;
    }

    @PostMapping("/ask")
    public Mono<String> post(@RequestBody AiRequest request) {
        return Mono.fromCallable(
                () -> {
                    System.out.println(
                            "Executing on: " + Thread.currentThread().getName()
                    );
                    return askAiUseCase.ask(request.question());
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(
            value = "/ask/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> stream(@RequestBody AiRequest request) {
//        $body = @{ question = "Hello" } | ConvertTo-Json
//
//        Invoke-WebRequest `
//        -Uri "http://localhost:8083/api/v1/ai/ask/stream" `
//        -Method POST `
//        -ContentType "application/json" `
//        -Headers @{ Accept = "text/event-stream" } `
//        -Body $body

        return askAiUseCase.stream(request.question());
    }
}
