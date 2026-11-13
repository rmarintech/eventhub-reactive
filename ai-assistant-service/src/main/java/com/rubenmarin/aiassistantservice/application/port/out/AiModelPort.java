package com.rubenmarin.aiassistantservice.application.port.out;

import reactor.core.publisher.Flux;

public interface AiModelPort {

    String generateResponse(String prompt);

    Flux<String> streamResponse(String prompt);
}
