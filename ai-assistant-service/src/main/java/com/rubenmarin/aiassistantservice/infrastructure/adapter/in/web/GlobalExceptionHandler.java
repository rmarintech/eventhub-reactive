package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

import com.rubenmarin.aiassistantservice.application.exception.EventNotFoundException;
import com.rubenmarin.aiassistantservice.application.exception.EventServiceUnavailableException;
import com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception.AiProviderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<Map<String, String>> handleAiProviderException(
            AiProviderException ex) {

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "AI service temporarily unavailable"
                ));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEventNotFound(
            EventNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "EVENT_NOT_FOUND",
                        "message", "The requested event was not found"
                ));
    }

    @ExceptionHandler(EventServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleEventServiceUnavailableException(
            EventServiceUnavailableException ex) {

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "EVENT_SERVICE_UNAVAILABLE",
                        "message", "Event service temporarily unavailable"
                ));
    }
}
