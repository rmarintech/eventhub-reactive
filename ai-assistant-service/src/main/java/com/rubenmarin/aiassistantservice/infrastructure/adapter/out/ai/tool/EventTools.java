package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool;

import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.exception.InvalidToolArgumentException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.Duration;
import java.util.UUID;

public class EventTools {

    private static final Logger log = LoggerFactory.getLogger(EventTools.class);
    /**
     * Spring AI uses the annotations to describe the tool to the LLM:
     * - Tool name: getAvailableSeats
     * - Description: Explains when the tool should be used.
     * - Parameter: eventId, a String representing the event identifier.
     * - Return value: An int containing the available seats.
     *
     */
    private final GetAvailableSeatsUseCase getAvailableSeatsUseCase;
    private final ObservationRegistry observationRegistry;

    public EventTools(
            GetAvailableSeatsUseCase getAvailableSeatsUseCase,
            ObservationRegistry observationRegistry
    ) {
        this.getAvailableSeatsUseCase = getAvailableSeatsUseCase;
        this.observationRegistry = observationRegistry;
    }

    //Spring AI discovers the methods annotated with @Tool
    // and makes their definitions available to the LLM.
    @Tool(description = "Retrieve the number of available seats for an EventHub event using its UUID")
    public int getAvailableSeats(
            @ToolParam(description = "The event's unique identifier in UUID format, e.g. 550e8400-e29b-41d4-a716-446655440000")
            String eventId
    ) {
        log.info("Tool executed for event: {}", eventId);

        UUID eventIdUUID;

        try {
            eventIdUUID = UUID.fromString(eventId);
        } catch (IllegalArgumentException e) {
            throw new InvalidToolArgumentException(eventId);
        }

        /**
         * New custom metric eventhub.ai.get_available_seats
         * */

        return Observation.createNotStarted(
                "eventhub.ai.get_available_seats",
                observationRegistry
        ).observe(() -> {
            // Your existing getAvailableSeats() logic goes here.
            Integer available = getAvailableSeatsUseCase
                    .getAvailableSeats(eventIdUUID)
                    .blockOptional(Duration.ofSeconds(10))
                    .orElseThrow(() -> new IllegalStateException("No availability data returned for event: " + eventId));

            log.info("Event {} has {} places available: ", eventId, available);
            return available;
        });

    }

}
