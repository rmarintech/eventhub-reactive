package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai;

import com.openai.errors.RateLimitException;
import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.DoubleAdder;

public class SpringAiAdapter implements AiModelPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAdapter.class);
    private final ChatClient chatClient;
    private final DoubleAdder totalCost = new DoubleAdder();

    public SpringAiAdapter(ChatClient.Builder builder, EventTools eventTools) {
        //defaultTools() registers the annotated methods of EventTools as
        // tools available to the model for requests made through this ChatClient.
        this.chatClient = builder
                .defaultTools(eventTools)
                .build();
    }

    @Override
    public String generateResponse(String prompt) {
//        return chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();

        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            Usage usage = response.getMetadata().getUsage();
            int inputTokens = usage.getPromptTokens();
            int outputTokens = usage.getCompletionTokens();
            int totalTokens = usage.getTotalTokens();

            log.info("Input tokens: {}", inputTokens);
            log.info("Output tokens: {}", outputTokens);
            log.info("Total tokens: {}", totalTokens);

            double inputCost = inputTokens * 0.05 / 1_000_000;
            double outputCost = outputTokens * 0.40 / 1_000_000;
            double requestCost = inputCost + outputCost;

            totalCost.add(requestCost);

            log.info("Request cost: ${}", String.format("%.8f", requestCost));
            log.info("Accumulated cost: ${}", String.format("%.8f", totalCost.sum()));

            return Objects.requireNonNull(response.getResult()).getOutput().getText();

        } catch (RateLimitException e) {
            log.error("OpenAI API rate limit or insufficient credits", e);
            throw new AiProviderException("AI service temporarily unavailable", e);
        }

    }

    @Override
    public Flux<String> streamResponse(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();

    }
}
