package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class FakeAiAdapter implements AiModelPort {

    private final EventTools eventTools;

    public FakeAiAdapter(EventTools eventTools) {
        this.eventTools = eventTools;
    }

    @Override
    public String generateResponse(String prompt) {
        int seats = eventTools.getAvailableSeats("123");

        return prompt + "? Event 123 has " + seats + " available seats.";

//        return "Fake Response to save tokens:" +
//                "Spring AI " +
//                "is a framework " +
//                "that simplifies " +
//                "AI integration " +
//                "in Java applications.";
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
