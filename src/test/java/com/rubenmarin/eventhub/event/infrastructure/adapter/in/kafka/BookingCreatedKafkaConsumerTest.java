package com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka;

import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
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

        UUID bookingId =  UUID.randomUUID();
        OffsetDateTime processedAt =  OffsetDateTime.now();

        Mockito.when(inboxRepository.save(Mockito.any(InboxEvent.class)))
                .thenReturn(Mono.error(bbddException))
                .thenReturn(Mono.error(bbddException))
                .thenReturn(Mono.just(new InboxEvent(bookingId,processedAt)));

        String payload ="{" +
                "\"bookingId\":\""  + bookingId + "\","+
                "\"eventId\":\"" + UUID.randomUUID() + "\","+
                "\"userId\":\"" + UUID.randomUUID() + "\""+
                "}";

        BookingCreatedKafkaConsumer consumer = new BookingCreatedKafkaConsumer(mapper, inboxRepository);
        Assertions.assertThrows(DataAccessResourceFailureException.class, () -> {consumer.consume(payload);});
        Assertions.assertThrows(DataAccessResourceFailureException.class, () -> {consumer.consume(payload);});

        consumer.consume(payload);
        Mockito.verify(inboxRepository, Mockito.times(3)).save(Mockito.any(InboxEvent.class));
    }
}
