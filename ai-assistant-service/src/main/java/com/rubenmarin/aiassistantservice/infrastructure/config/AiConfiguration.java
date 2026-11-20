package com.rubenmarin.aiassistantservice.infrastructure.config;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.application.service.AiService;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.FakeAiAdapter;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.SpringAiAdapter;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventToolExceptionProcessor;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfiguration {


    @Bean
    public AskAiUseCase askAiUseCase(AiModelPort aiModelPort) {
        return new AiService(aiModelPort);
    }

    @Bean
    @Profile("prod")
    public AiModelPort springAiModelPort(
            ChatClient.Builder builder,
            EventTools eventTools
    ) {
        return new SpringAiAdapter(builder, eventTools);
    }

    @Bean
    //@Profile("dev")
    @Profile("!prod")
    public AiModelPort fakeAiModelPort(EventTools eventTools) {
        return new FakeAiAdapter(eventTools);
    }

    @Bean
    public EventTools eventTools(
            GetAvailableSeatsUseCase getAvailableSeatsUseCase,
            ObservationRegistry observationRegistry
    ) {
        return new EventTools(getAvailableSeatsUseCase, observationRegistry);
    }

    @Bean
    public ToolExecutionExceptionProcessor eventToolExceptionProcessor() {
        return new EventToolExceptionProcessor();
    }

}
