package com.rubenmarin.bookingservice.application.service;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.bookingservice.application.model.InboxEvent;
import com.rubenmarin.bookingservice.application.port.in.ProcessBookingCreatedUseCase;
import com.rubenmarin.bookingservice.application.port.out.InboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Transactional
@Service
public class ProcessBookingCreatedService implements ProcessBookingCreatedUseCase {

    private final InboxRepository inboxRepository;
    public ProcessBookingCreatedService(InboxRepository inboxRepository){
        this.inboxRepository = inboxRepository;
    }

    @Override
    public void process(BookingCreatedIntegrationEvent bookingCreatedIntegrationEvent) {

        UUID eventId = bookingCreatedIntegrationEvent.eventId();
        UUID userId = bookingCreatedIntegrationEvent.userId();
        UUID bookingId = bookingCreatedIntegrationEvent.bookingId();
        UUID integrationEventId = bookingCreatedIntegrationEvent.integrationEventId();

        if(!inboxRepository.existsById(integrationEventId)){

            //Guardarlo en el inbox para que no se vuelva a procesar
            InboxEvent inboxEvent = new InboxEvent(integrationEventId, OffsetDateTime.now());
            inboxRepository.save(inboxEvent);

            //TODO: procesar el evento con su lógica de negocio

        }
    }
}
