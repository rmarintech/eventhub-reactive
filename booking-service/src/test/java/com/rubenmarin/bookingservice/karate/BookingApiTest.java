package com.rubenmarin.bookingservice.karate;

import io.karatelabs.junit6.Karate;
import org.junit.jupiter.api.DynamicNode;

class BookingApiTest {

     @Karate.Test
     Iterable<DynamicNode> testBookingApi() {
         return Karate.run("booking").relativeTo(getClass());
     }
}
