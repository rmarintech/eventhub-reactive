package com.rubenmarin.aiassistantservice.application.service;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import reactor.core.publisher.Flux;

public class AiService implements AskAiUseCase {

    private final AiModelPort aiModelPort;

    public AiService(AiModelPort aiModelPort) {
        this.aiModelPort = aiModelPort;

    }

    @Override
    public String ask(String question) {
        return aiModelPort.generateResponse(question);
    }

    @Override
    public Flux<String> stream(String question){
        return aiModelPort.streamResponse(question);
    }
}
