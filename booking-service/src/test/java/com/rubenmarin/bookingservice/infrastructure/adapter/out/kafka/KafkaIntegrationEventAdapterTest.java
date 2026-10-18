package com.rubenmarin.bookingservice.infrastructure.adapter.out.kafka;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.event.IntegrationEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class KafkaIntegrationEventAdapterTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    private KafkaIntegrationEventAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new KafkaIntegrationEventAdapter(
                kafkaTemplate,
                objectMapper
        );
    }

    @Test
    void shouldPublishBookingCreatedEvent() {

        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent = new BookingCreatedIntegrationEvent(bookingId, eventId, userId);

        adapter.publish(bookingCreatedIntegrationEvent);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);


        Mockito.verify(kafkaTemplate).send(
                Mockito.eq("booking-created"),
                Mockito.eq(bookingId.toString()),
                captor.capture());

        String captured = captor.getValue();

        System.out.println(captured);

         JsonNode json = objectMapper.readTree(captured);

        Assertions.assertEquals(
                bookingId.toString(),
                json.get("bookingId").asString()
        );

        Assertions.assertEquals(
                eventId.toString(),
                json.get("eventId").asString()
        );

        Assertions.assertEquals(
                userId.toString(),
                json.get("userId").asString()
        );


    }

    @Test
    void shouldRejectUnsupportedIntegrationEvent() {
        IntegrationEvent unsupportedEvent = new IntegrationEvent() {};

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> adapter.publish(unsupportedEvent)
        );

        Mockito.verifyNoInteractions(kafkaTemplate);
    }
}
