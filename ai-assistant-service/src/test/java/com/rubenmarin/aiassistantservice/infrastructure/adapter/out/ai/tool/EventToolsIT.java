package com.rubenmarin.aiassistantservice.infrastructure.adapter.out.ai.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
 class EventToolsIT {

    @Autowired
    EventTools eventTools;

    @Test
    void shouldRetrieveRealEventAvailability(){

        String uuid = "0ebbca7e-87bb-4c1d-915c-676b59ffe329";
        int available = eventTools.getAvailableSeats(uuid);

        Assertions.assertTrue(available >= 0);
    }
}
