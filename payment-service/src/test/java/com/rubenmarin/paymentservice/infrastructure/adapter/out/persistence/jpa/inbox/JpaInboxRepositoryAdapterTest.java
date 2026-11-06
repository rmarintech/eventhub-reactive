package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class JpaInboxRepositoryAdapterTest {

    @Mock
    SpringDataInboxRepository springDataInboxRepository;

    @InjectMocks
    JpaInboxRepositoryAdapter jpaInboxRepositoryAdapter;

    @Test
    void shouldRegisterNewInboxEvent() {
        UUID integrationEventId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        InboxEvent inboxEvent = new InboxEvent(integrationEventId, receivedAt);

        Mockito.when(springDataInboxRepository.insertIfNotExists(integrationEventId, receivedAt))
                .thenReturn(1);

        boolean registered = jpaInboxRepositoryAdapter.register(inboxEvent);

        Assertions.assertTrue(registered);

        Mockito.verify(springDataInboxRepository).insertIfNotExists(integrationEventId, receivedAt);
    }

    @Test
    void shouldReturnFalseWhenInboxEventAlreadyExists() {
        UUID integrationEventId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        InboxEvent inboxEvent = new InboxEvent(integrationEventId, receivedAt);

        Mockito.when(springDataInboxRepository.insertIfNotExists(integrationEventId, receivedAt))
                .thenReturn(0);

        boolean registered = jpaInboxRepositoryAdapter.register(inboxEvent);

        Assertions.assertFalse(registered);

        Mockito.verify(springDataInboxRepository).insertIfNotExists(integrationEventId, receivedAt);
    }
}
