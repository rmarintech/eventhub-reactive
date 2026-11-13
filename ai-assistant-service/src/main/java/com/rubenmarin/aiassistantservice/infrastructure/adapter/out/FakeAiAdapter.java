package com.rubenmarin.aiassistantservice.infrastructure.adapter.out;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class FakeAiAdapter implements AiModelPort {

    @Override
    public String generateResponse(String prompt) {
        return "Fake Response to save tokens:" +
                "Spring AI " +
                "is a framework " +
                "that simplifies " +
                "AI integration " +
                "in Java applications.";
    }

    @Override
    public Flux<String> streamResponse(String prompt) {
        return Flux.just(
                "Fake Response to save tokens:",
                "Spring AI ",
                "is a framework ",
                "that simplifies ",
                "AI integration ",
                "in Java applications."
        ).delayElements(Duration.ofMillis(500));
    }
}
