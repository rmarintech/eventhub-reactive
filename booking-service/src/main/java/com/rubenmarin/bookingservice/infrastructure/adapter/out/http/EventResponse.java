package com.rubenmarin.bookingservice.infrastructure.adapter.out.http;

import java.util.UUID;

public record EventResponse(UUID id,
                            String status,
                            int available) {


}
