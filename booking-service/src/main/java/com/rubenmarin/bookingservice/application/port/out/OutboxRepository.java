package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.application.model.BrokerName;
import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEventId;

import java.util.List;

public interface OutboxRepository {

    public OutboxEvent createOutboxEvent(OutboxEvent event);

    public List<OutboxEvent> findByStatusPendingAndBrokerName(BrokerName brokerName);

    public OutboxEvent markAsPublished (OutboxEventId outboxEventId);
}
