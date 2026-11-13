package com.rubenmarin.aiassistantservice.infrastructure.config;

import com.rubenmarin.aiassistantservice.application.port.in.AskAiUseCase;
import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.application.service.AiService;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.FakeAiAdapter;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.SpringAiAdapter;
import org.springframework.ai.chat.client.ChatClient;
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
    public AiModelPort springAiModelPort(ChatClient.Builder builder) {
        return new SpringAiAdapter(builder);
    }

    @Bean
    //@Profile("dev")
    @Profile("!prod")
    public AiModelPort fakeAiModelPort() {
        return new FakeAiAdapter();
    }
}
