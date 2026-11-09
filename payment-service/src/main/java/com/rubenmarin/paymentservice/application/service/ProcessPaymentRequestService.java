package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import com.rubenmarin.paymentservice.application.model.IntegrationEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventId;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.port.out.*;
import com.rubenmarin.paymentservice.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class ProcessPaymentRequestService implements ProcessPaymentRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentRequestService.class);
    private final InboxRepository inboxRepository;
    private final PaymentRepository paymentRepository;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final OutboxRepository outboxRepository;
    private final PaymentProcessor paymentProcessor;
    private final IntegrationEventSerializer integrationEventSerializer;

    public ProcessPaymentRequestService(
            InboxRepository inboxRepository,
            PaymentRepository paymentRepository,
            CreatePaymentUseCase createPaymentUseCase,
            OutboxRepository outboxRepository,
            PaymentProcessor paymentProcessor,
            IntegrationEventSerializer integrationEventSerializer
    ) {
        this.inboxRepository = inboxRepository;
        this.paymentRepository = paymentRepository;
        this.createPaymentUseCase = createPaymentUseCase;
        this.outboxRepository = outboxRepository;
        this.paymentProcessor = paymentProcessor;
        this.integrationEventSerializer = integrationEventSerializer;
    }

    //FIXME: Transactional decorator
    @Override
    public void processPaymentRequest(ProcessPaymentRequestCommand processPaymentRequestCommand) {
        UUID integrationEventId = processPaymentRequestCommand.integrationEventId();
        Instant receivedAt = Instant.now();
        InboxEvent inboxEvent = new InboxEvent(integrationEventId, receivedAt);

        // Inbox register event for creating payment request
        // Create Payment Request
        // Process Payment
        // Outbox payment failed/succeeded

        boolean registeredAsNew = inboxRepository.register(inboxEvent);
        if (registeredAsNew) {
            BookingId bookingId = processPaymentRequestCommand.bookingId();
            PaymentRequestId paymentRequestId = processPaymentRequestCommand.paymentRequestId();
            Money money = processPaymentRequestCommand.money();

            CreatePaymentCommand createPaymentCommand = new CreatePaymentCommand(bookingId, paymentRequestId, money);
            Payment payment = createPaymentUseCase.create(createPaymentCommand);

            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                log.info("Processing payment");
                boolean paymentSucceed = paymentProcessor.process(payment);
                if (paymentSucceed) {
                    payment.markAsCompleted();
                } else {
                    payment.markAsFailed();
                }
                paymentRepository.save(payment);
            }

            String topic = "";
            String eventType = "";

            String messageKey = payment.getBookingId().bookingId().toString();
            UUID aggregateId = payment.getPaymentId().paymentId();

            if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                eventType = "PaymentCompleted";
                topic = "payment-completed";
            } else if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
                eventType = "PaymentFailed";
                topic = "payment-failed";
            } else {
                throw new IllegalStateException("Unknown payment status: " + payment.getPaymentStatus());
            }

            // Utilizamos el mismo UUID para OutboxEvent.id e IntegrationEvent.integrationEventId.
            OutboxEventId outboxId = OutboxEventId.generate();

            IntegrationEvent integrationEvent = new IntegrationEvent(
                    outboxId.outboxEventId(),
                    paymentRequestId.paymentRequestId(),
                    bookingId.bookingId(),
                    payment.getPaymentId().paymentId(),
                    payment.getPaymentStatus());

            String payload = integrationEventSerializer.serialize(integrationEvent);

            OutboxEvent outboxEvent = OutboxEvent.pending(outboxId, messageKey, eventType, aggregateId, topic, payload);
            outboxRepository.save(outboxEvent);
        }
    }

}
