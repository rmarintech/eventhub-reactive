package com.rubenmarin.bookingservice.application.port.out;

import com.rubenmarin.bookingservice.application.model.InboxEvent;

import java.util.UUID;

public interface InboxRepository {
    //    comprobar si un evento ya ha sido procesado.
    public boolean existsById(UUID eventId);

    //registrar el evento como procesado.
    public InboxEvent save(InboxEvent event);
}
