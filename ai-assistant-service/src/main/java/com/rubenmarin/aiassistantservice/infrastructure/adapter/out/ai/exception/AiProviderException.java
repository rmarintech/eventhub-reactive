package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.exception;

public class AiProviderException extends RuntimeException {
    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
