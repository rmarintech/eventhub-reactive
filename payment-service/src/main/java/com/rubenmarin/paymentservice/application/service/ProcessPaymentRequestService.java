package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;

import java.time.Instant;
import java.util.UUID;

public class ProcessPaymentRequestService implements ProcessPaymentRequestUseCase {

    private final InboxRepository inboxRepository;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final OutboxRepository outboxRepository;

    public ProcessPaymentRequestService(
            InboxRepository inboxRepository,
            CreatePaymentUseCase createPaymentUseCase,
            OutboxRepository outboxRepository
    ) {
        this.inboxRepository = inboxRepository;
        this.createPaymentUseCase = createPaymentUseCase;
        this.outboxRepository = outboxRepository;
    }

    //FIXME: Transactional decorator
    @Override
    public void processPaymentRequest(ProcessPaymentRequestCommand  processPaymentRequestCommand) {
        UUID integrationEventId= processPaymentRequestCommand.integrationEventId();
        Instant receivedAt = Instant.now();
        InboxEvent inboxEvent = new InboxEvent(integrationEventId , receivedAt);

        boolean registeredAsNew = inboxRepository.register(inboxEvent);
        if (registeredAsNew) {
            BookingId bookingId = processPaymentRequestCommand.bookingId();
            PaymentRequestId paymentRequestId  = processPaymentRequestCommand.paymentRequestId();
            Money money = processPaymentRequestCommand.money();

            CreatePaymentCommand createPaymentCommand = new  CreatePaymentCommand(bookingId, paymentRequestId, money);
            createPaymentUseCase.create(createPaymentCommand);
            //TODO: outboxRepository.createOutboxEvent
        }
    }

}
