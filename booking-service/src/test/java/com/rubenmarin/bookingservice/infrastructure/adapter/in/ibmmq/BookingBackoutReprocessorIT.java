package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingBackoutReprocessorIT {

    @Autowired
    BookingBackoutReprocessor reprocessor;

   // @Test
    void shouldReprocessBackoutMessage() {
        reprocessor.reprocess(
                "ID:414d5120514d312020202020202020203e4bd66a013c0040"
        );
    }
}
