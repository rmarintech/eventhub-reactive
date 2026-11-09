package com.rubenmarin.paymentservice.application.port.out;

import com.rubenmarin.paymentservice.application.model.IntegrationEvent;

public interface IntegrationEventSerializer {

    String serialize (IntegrationEvent integrationEvent);
}
