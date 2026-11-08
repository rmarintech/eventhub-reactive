package com.rubenmarin.paymentservice.application.port.out;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;

import java.util.List;

public interface OutboxRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPendingEvents();
}
