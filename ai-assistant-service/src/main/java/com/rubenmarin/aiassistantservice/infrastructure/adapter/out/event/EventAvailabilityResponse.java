package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventAvailabilityResponse(
        Integer availableCapacity
) {
}
