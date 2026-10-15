package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.domain.model.EventId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Tag("external")
@SpringBootTest
class RestEventAvailabilityAdapterIT {

    @Autowired
    EventAvailabilityPort eventAvailabilityPort;

    @Test
    void shouldCheckEventAvailability() {

        UUID uuid = UUID.fromString("123a5146-61ce-4680-af2d-9c4eaa362eba");
        EventId eventId = new EventId(uuid);

        boolean available = eventAvailabilityPort.isAvailable(eventId);

        Assertions.assertTrue(available);
    }
}