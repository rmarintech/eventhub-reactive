package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool;

import com.rubenmarin.aiassistantservice.application.exception.EventNotFoundException;
import com.rubenmarin.aiassistantservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.exception.InvalidToolArgumentException;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

public class EventToolExceptionProcessor implements ToolExecutionExceptionProcessor {

    @Override
    public @NonNull String process(ToolExecutionException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof EventNotFoundException) {
            return "EVENT_NOT_FOUND: The requested event does not exist.";
        }
        if (cause instanceof EventServiceUnavailableException) {
            return "EVENT_SERVICE_UNAVAILABLE: Event service is not available at the moment";
        }
        if (cause instanceof InvalidToolArgumentException) {
            return "INVALID_EVENT_ID: The event identifier must be a valid UUID.";
        }

        throw exception;
    }
}
