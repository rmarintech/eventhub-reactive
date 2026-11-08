package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class JpaOutboxRepositoryAdapterTest {

    @Mock
    private SpringDataOutboxRepository springDataOutboxRepository;

    @InjectMocks
    private JpaOutboxRepositoryAdapter jpaOutboxRepositoryAdapter;

    @Test
    void shouldSaveOutboxEvent() {

        // Given
        OutboxEvent outboxEvent = OutboxEvent.pending(
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                """
                        {
                            "status": "COMPLETED"
                        }
                        """
        );

        OutboxJpaEntity entity = OutboxMapper.toEntity(outboxEvent);

        Mockito.when(springDataOutboxRepository.save(Mockito.any(OutboxJpaEntity.class)))
                .thenReturn(entity);

        // When
        OutboxEvent savedEvent = jpaOutboxRepositoryAdapter.save(outboxEvent);

        // Then
        Assertions.assertEquals(outboxEvent, savedEvent);

        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);

        Mockito.verify(springDataOutboxRepository).save(captor.capture());

        OutboxJpaEntity capturedEntity = captor.getValue();

        Assertions.assertEquals(outboxEvent.id().id(), capturedEntity.getId());
        Assertions.assertEquals(outboxEvent.messageKey(), capturedEntity.getMessageKey());
        Assertions.assertEquals(outboxEvent.eventType(), capturedEntity.getEventType());
        Assertions.assertEquals(outboxEvent.aggregateId(), capturedEntity.getAggregateId());
        Assertions.assertEquals(outboxEvent.topic(), capturedEntity.getTopic());
        Assertions.assertEquals(outboxEvent.payload(), capturedEntity.getPayload());
        Assertions.assertEquals(OutboxEventStatus.PENDING, capturedEntity.getStatus());
        Assertions.assertEquals(outboxEvent.createdAt(), capturedEntity.getCreatedAt());
        Assertions.assertNull(capturedEntity.getPublishedAt());
    }

    @Test
    void shouldFindPendingEventsOrderedByCreatedAt() {

        // Given
        OutboxEvent olderEvent = OutboxEvent.pending(
                "booking-1",
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                "{}"
        );

        OutboxEvent newerEvent = OutboxEvent.pending(
                "booking-2",
                "PaymentFailed",
                UUID.randomUUID(),
                "payment-failed",
                "{}"
        );

        List<OutboxJpaEntity> entities = List.of(
                OutboxMapper.toEntity(olderEvent),
                OutboxMapper.toEntity(newerEvent)
        );

        Mockito.when(springDataOutboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(entities);

        // When
        List<OutboxEvent> result = jpaOutboxRepositoryAdapter.findPendingEvents();

        // Then
        Assertions.assertEquals(2, result.size());

        Assertions.assertEquals(olderEvent.id(), result.get(0).id());
        Assertions.assertEquals(newerEvent.id(), result.get(1).id());

        Assertions.assertEquals(olderEvent, result.get(0));
        Assertions.assertEquals(newerEvent, result.get(1));

        Mockito.verify(springDataOutboxRepository).findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
    }
}