package com.rubenmarin.aiassistantservice.infrastructure.adapter.out;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public class SpringAiAdapter implements AiModelPort {

    private final ChatClient chatClient;

    public SpringAiAdapter(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generateResponse(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> streamResponse(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();

    }
}
