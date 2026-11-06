package com.rubenmarin.paymentservice.application.port.out;

import com.rubenmarin.paymentservice.application.model.InboxEvent;

public interface InboxRepository {
    /**
     *
     * @param inboxEvent
     * @return
     * true: el evento se ha registrado por primera vez.
     * false: el evento ya estaba registrado.
     * Una excepción: se ha producido un error de persistencia.
     */
    boolean register(InboxEvent inboxEvent);
}
