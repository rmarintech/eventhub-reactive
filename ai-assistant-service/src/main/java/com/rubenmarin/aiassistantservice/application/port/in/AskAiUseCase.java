package com.rubenmarin.aiassistantservice.application.port.in;

import reactor.core.publisher.Flux;

public interface AskAiUseCase {

    String ask(String question);

    Flux<String> stream(String question);
}
