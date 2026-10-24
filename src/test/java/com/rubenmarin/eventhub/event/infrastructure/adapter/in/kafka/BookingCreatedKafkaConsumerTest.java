package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BookingCreatedKafkaConsumerTest {

    @Mock
    private InboxRepository inboxRepository;

    private ObjectMapper mapper = new JsonMapper();

    @Test
    void shouldFail2TriesAndThenSuccessRecovering() {
        DataAccessResourceFailureException bbddException =
                new DataAccessResourceFailureException("PostgreSQL unavailable");

        UUID integrationEventId = UUID.randomUUID();
        OffsetDateTime processedAt = OffsetDateTime.now();

        Mockito.when(inboxRepository.save(Mockito.any(InboxEvent.class)))
                .thenReturn(Mono.error(bbddException))
                .thenReturn(Mono.error(bbddException))
                .thenReturn(Mono.just(new InboxEvent(integrationEventId, processedAt)));

        String payload = "{" +
                "\"integrationEventId\":\"" + integrationEventId + "\"," +
                "\"bookingId\":\"" + UUID.randomUUID() + "\"," +
                "\"eventId\":\"" + UUID.randomUUID() + "\"," +
                "\"userId\":\"" + UUID.randomUUID() + "\"" +
                "}";

        BookingCreatedKafkaConsumer consumer = new BookingCreatedKafkaConsumer(mapper, inboxRepository);
        Assertions.assertThrows(DataAccessResourceFailureException.class, () -> {
            consumer.consume(payload);
        });
        Assertions.assertThrows(DataAccessResourceFailureException.class, () -> {
            consumer.consume(payload);
        });

        consumer.consume(payload);
        Mockito.verify(inboxRepository, Mockito.times(3)).save(Mockito.any(InboxEvent.class));
    }

    @Test
    void shouldProcessDifferentIntegrationEventsWithSameBookingId (){

        UUID integrationEvent1Id = UUID.randomUUID();
        UUID integrationEvent2Id = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime processedAt = OffsetDateTime.now();

        String payload1 = "{" +
                "\"integrationEventId\":\"" + integrationEvent1Id + "\"," +
                "\"bookingId\":\"" + bookingId + "\"," +
                "\"eventId\":\"" + eventId + "\"," +
                "\"userId\":\"" + userId + "\"" +
                "}";

        String payload2 = "{" +
                "\"integrationEventId\":\"" + integrationEvent2Id + "\"," +
                "\"bookingId\":\"" + bookingId + "\"," +
                "\"eventId\":\"" + eventId + "\"," +
                "\"userId\":\"" + userId + "\"" +
                "}";

        BookingCreatedKafkaConsumer consumer = new BookingCreatedKafkaConsumer(mapper, inboxRepository);

        Mockito.when(inboxRepository.save(Mockito.any(InboxEvent.class)))
                .thenReturn(Mono.just(new InboxEvent(integrationEvent1Id, processedAt)));
        consumer.consume(payload1);

        Mockito.when(inboxRepository.save(Mockito.any(InboxEvent.class)))
                .thenReturn(Mono.just(new InboxEvent(integrationEvent2Id, processedAt)));
        consumer.consume(payload2);


        ArgumentCaptor<InboxEvent> captor = ArgumentCaptor.forClass(InboxEvent.class);
        Mockito.verify(inboxRepository,Mockito.times(2)).save(captor.capture());
        List<InboxEvent> inboxEventList = captor.getAllValues();
        Assertions.assertEquals(integrationEvent1Id,inboxEventList.get(0).integrationEventId());
        Assertions.assertEquals(integrationEvent2Id,inboxEventList.get(1).integrationEventId());

        Assertions.assertNotEquals(
                inboxEventList.get(0).integrationEventId(),
                inboxEventList.get(1).integrationEventId()
        );
    }
}
