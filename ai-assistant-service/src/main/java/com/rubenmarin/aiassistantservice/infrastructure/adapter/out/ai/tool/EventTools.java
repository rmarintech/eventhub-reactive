package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class EventTools {

    /**
     * Spring AI uses the annotations to describe the tool to the LLM:
     * - Tool name: getAvailableSeats
     * - Description: Explains when the tool should be used.
     * - Parameter: eventId, a String representing the event identifier.
     * - Return value: An int containing the available seats.
     * */

    //Spring AI discovers the methods annotated with @Tool
    // and makes their definitions available to the LLM.
    @Tool(description = "Get the number of available seats for an event")
    public int getAvailableSeats(
            @ToolParam(description = "The event identifier") String eventId
    ) {
        System.out.println("Tool executed for event: " + eventId);
        return 25;
    }

}
