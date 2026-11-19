package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool.EventTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FakeAiAdapter implements AiModelPort {

    private static final Logger log = LoggerFactory.getLogger(FakeAiAdapter.class);
    private final EventTools eventTools;

    public FakeAiAdapter(EventTools eventTools) {
        this.eventTools = eventTools;
    }

    @Override
    public String generateResponse(String prompt) {

        log.info("FakeAiAdapter - generateResponse for {}", prompt);
        Pattern pattern = Pattern.compile(
                "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
        );

        Matcher matcher = pattern.matcher(prompt);
        UUID eventId = UUID.randomUUID();
        if (matcher.find()) {
            eventId = UUID.fromString(matcher.group());
            log.info("FakeAiAdapter - eventId{}", eventId);
        }

        int seats = eventTools.getAvailableSeats(eventId.toString());

        return prompt + "? Event " + eventId.toString() + " has " + seats + " available seats.";

//        return "Fake Response to save tokens:" +
//                "Spring AI " +
//                "is a framework " +
//                "that simplifies " +
//                "AI integration " +
//                "in Java applications.";
    }

    @Override
    public Flux<String> streamResponse(String prompt) {

        log.info("FakeAiAdapter - streamResponse for {}", prompt);

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
