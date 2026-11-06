package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class JpaInboxRepositoryAdapter implements InboxRepository {

    private final SpringDataInboxRepository springDataInboxRepository;

    public JpaInboxRepositoryAdapter(SpringDataInboxRepository springDataInboxRepository) {
        this.springDataInboxRepository = springDataInboxRepository;
    }

    /**
     *
     * @param inboxEvent
     * @return true: el evento se ha registrado por primera vez. (Se inserta)
     * false: el evento ya estaba registrado. (Do nothing)
     * Una excepción: se ha producido un error de persistencia.
     */
    @Override
    public boolean register(InboxEvent inboxEvent) {

        UUID integrationEventId = inboxEvent.integrationEventId();
        Instant receivedAt = inboxEvent.receivedAt();
        int insertedRows = springDataInboxRepository.insertIfNotExists(integrationEventId, receivedAt);

        return insertedRows == 1;

    }
}
