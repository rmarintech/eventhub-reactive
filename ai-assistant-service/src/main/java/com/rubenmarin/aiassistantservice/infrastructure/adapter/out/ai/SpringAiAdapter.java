package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public class SpringAiAdapter implements AiModelPort {

    private final ChatClient chatClient;

    public SpringAiAdapter(ChatClient.Builder builder, EventTools eventTools) {
        //defaultTools() registers the annotated methods of EventTools as
        // tools available to the model for requests made through this ChatClient.
        this.chatClient = builder
                .defaultTools(eventTools)
                .build();
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
