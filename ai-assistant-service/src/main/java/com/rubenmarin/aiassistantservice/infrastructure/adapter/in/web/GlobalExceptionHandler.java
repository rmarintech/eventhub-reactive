package com.rubenmarin.aiassistantservice.infrastructure.adapter.in.web;

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
}
