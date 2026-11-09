package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.model.InboxEvent;
import com.rubenmarin.paymentservice.application.model.IntegrationEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import com.rubenmarin.paymentservice.application.port.out.IntegrationEventSerializer;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import com.rubenmarin.paymentservice.application.port.out.PaymentProcessor;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentRequestServiceTest {

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CreatePaymentUseCase createPaymentUseCase;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private IntegrationEventSerializer integrationEventSerializer;

    @InjectMocks
    private ProcessPaymentRequestService processPaymentRequestService;

    // =========================================================
    // TEST 1 - PaymentCompleted
    // =========================================================

    @Test
    void shouldProcessSuccessfulPaymentAndCreateCompletedOutboxEvent() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("150.00");

        Payment payment = Payment.create(command.bookingId(), command.paymentRequestId(), command.money());

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(true);
        Mockito.when(createPaymentUseCase.create(Mockito.any(CreatePaymentCommand.class))).thenReturn(payment);
        Mockito.when(paymentProcessor.process(payment)).thenReturn(true);
        Mockito.when(integrationEventSerializer.serialize(Mockito.any(IntegrationEvent.class))).thenReturn("{\"status\":\"COMPLETED\"}");

        // WHEN
        processPaymentRequestService.processPaymentRequest(command);

        // THEN
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment.getPaymentStatus());

        Mockito.verify(paymentProcessor).process(payment);
        Mockito.verify(paymentRepository).save(payment);

        IntegrationEvent integrationEvent = captureIntegrationEvent();
        OutboxEvent outboxEvent = captureOutboxEvent();

        assertPaymentResult(
                command,
                payment,
                integrationEvent,
                outboxEvent,
                PaymentStatus.COMPLETED,
                "PaymentCompleted",
                "payment-completed",
                "{\"status\":\"COMPLETED\"}"
        );

        verifyCreatePaymentCommand(command);
    }

    // =========================================================
    // TEST 2 - PaymentFailed
    // =========================================================

    @Test
    void shouldProcessFailedPaymentAndCreateFailedOutboxEvent() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("47.77");

        Payment payment = Payment.create(command.bookingId(), command.paymentRequestId(), command.money());

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(true);
        Mockito.when(createPaymentUseCase.create(Mockito.any(CreatePaymentCommand.class))).thenReturn(payment);
        Mockito.when(paymentProcessor.process(payment)).thenReturn(false);
        Mockito.when(integrationEventSerializer.serialize(Mockito.any(IntegrationEvent.class))).thenReturn("{\"status\":\"FAILED\"}");

        // WHEN
        processPaymentRequestService.processPaymentRequest(command);

        // THEN
        Assertions.assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());

        Mockito.verify(paymentProcessor).process(payment);
        Mockito.verify(paymentRepository).save(payment);

        IntegrationEvent integrationEvent = captureIntegrationEvent();
        OutboxEvent outboxEvent = captureOutboxEvent();

        assertPaymentResult(
                command,
                payment,
                integrationEvent,
                outboxEvent,
                PaymentStatus.FAILED,
                "PaymentFailed",
                "payment-failed",
                "{\"status\":\"FAILED\"}"
        );

        verifyCreatePaymentCommand(command);
    }

    // =========================================================
    // TEST 3 - Inbox duplicado
    // =========================================================

    @Test
    void shouldIgnoreDuplicatedInboxEvent() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("150.00");

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(false);

        // WHEN
        processPaymentRequestService.processPaymentRequest(command);

        // THEN
        Mockito.verify(inboxRepository, Mockito.times(1)).register(Mockito.any(InboxEvent.class));

        Mockito.verifyNoInteractions(createPaymentUseCase, paymentProcessor, paymentRepository, integrationEventSerializer, outboxRepository);
    }

    // =========================================================
    // TEST 4 - Payment previamente completado
    // =========================================================

    @Test
    void shouldNotReprocessAlreadyCompletedPayment() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("150.00");

        Payment existingPayment = Payment.create(command.bookingId(), command.paymentRequestId(), command.money());

        existingPayment.markAsCompleted();

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(true);
        Mockito.when(createPaymentUseCase.create(Mockito.any(CreatePaymentCommand.class))).thenReturn(existingPayment);
        Mockito.when(integrationEventSerializer.serialize(Mockito.any(IntegrationEvent.class))).thenReturn("{\"status\":\"COMPLETED\"}");

        // WHEN
        processPaymentRequestService.processPaymentRequest(command);

        // THEN
        Assertions.assertEquals(PaymentStatus.COMPLETED, existingPayment.getPaymentStatus());

        Mockito.verifyNoInteractions(paymentProcessor);
        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any(Payment.class));

        IntegrationEvent integrationEvent = captureIntegrationEvent();
        OutboxEvent outboxEvent = captureOutboxEvent();

        assertPaymentResult(
                command,
                existingPayment,
                integrationEvent,
                outboxEvent,
                PaymentStatus.COMPLETED,
                "PaymentCompleted",
                "payment-completed",
                "{\"status\":\"COMPLETED\"}"
        );

        verifyCreatePaymentCommand(command);
    }

    // =========================================================
    // TEST 5 - Payment previamente fallido
    // =========================================================

    @Test
    void shouldNotReprocessAlreadyFailedPayment() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("47.77");

        Payment existingPayment = Payment.create(command.bookingId(), command.paymentRequestId(), command.money());

        existingPayment.markAsFailed();

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(true);
        Mockito.when(createPaymentUseCase.create(Mockito.any(CreatePaymentCommand.class))).thenReturn(existingPayment);
        Mockito.when(integrationEventSerializer.serialize(Mockito.any(IntegrationEvent.class))).thenReturn("{\"status\":\"FAILED\"}");

        // WHEN
        processPaymentRequestService.processPaymentRequest(command);

        // THEN
        Assertions.assertEquals(PaymentStatus.FAILED, existingPayment.getPaymentStatus());

        Mockito.verifyNoInteractions(paymentProcessor);

        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any(Payment.class));

        IntegrationEvent integrationEvent = captureIntegrationEvent();
        OutboxEvent outboxEvent = captureOutboxEvent();

        assertPaymentResult(
                command,
                existingPayment,
                integrationEvent,
                outboxEvent,
                PaymentStatus.FAILED,
                "PaymentFailed",
                "payment-failed",
                "{\"status\":\"FAILED\"}"
        );

        verifyCreatePaymentCommand(command);
    }

    // =========================================================
    // TEST 6 - Error técnico del PaymentProcessor
    // =========================================================

    @Test
    void shouldPropagateTechnicalFailureWithoutCreatingOutboxEvent() {

        // GIVEN
        ProcessPaymentRequestCommand command = createCommand("150.00");

        Payment payment = Payment.create(command.bookingId(), command.paymentRequestId(), command.money());

        Mockito.when(inboxRepository.register(Mockito.any(InboxEvent.class))).thenReturn(true);
        Mockito.when(createPaymentUseCase.create(Mockito.any(CreatePaymentCommand.class))).thenReturn(payment);
        Mockito.when(paymentProcessor.process(payment)).thenThrow(new IllegalStateException("Payment processor unavailable"));

        // WHEN & THEN
        IllegalStateException exception =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> processPaymentRequestService.processPaymentRequest(command)
                );

        Assertions.assertEquals("Payment processor unavailable", exception.getMessage());

        Assertions.assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());

        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any(Payment.class));
        Mockito.verifyNoInteractions(integrationEventSerializer, outboxRepository);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private ProcessPaymentRequestCommand createCommand(String amount) {
        return new ProcessPaymentRequestCommand(
                UUID.randomUUID(),
                BookingId.generateBookingId(),
                PaymentRequestId.generatePaymentRequestId(),
                new Money(new BigDecimal(amount),
                        Currency.getInstance("EUR")
                )
        );
    }

    private IntegrationEvent captureIntegrationEvent() {
        ArgumentCaptor<IntegrationEvent> captor = ArgumentCaptor.forClass(IntegrationEvent.class);
        Mockito.verify(integrationEventSerializer).serialize(captor.capture());
        return captor.getValue();
    }

    private OutboxEvent captureOutboxEvent() {

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        Mockito.verify(outboxRepository).save(captor.capture());
        return captor.getValue();
    }

    private void verifyCreatePaymentCommand(ProcessPaymentRequestCommand expectedCommand) {

        ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);

        Mockito.verify(createPaymentUseCase).create(captor.capture());

        CreatePaymentCommand actualCommand = captor.getValue();

        Assertions.assertEquals(expectedCommand.bookingId(), actualCommand.bookingId());
        Assertions.assertEquals(expectedCommand.paymentRequestId(), actualCommand.paymentRequestId());
        Assertions.assertEquals(expectedCommand.money(), actualCommand.money());
    }

    private void assertPaymentResult(
            ProcessPaymentRequestCommand command,
            Payment payment,
            IntegrationEvent integrationEvent,
            OutboxEvent outboxEvent,
            PaymentStatus expectedStatus,
            String expectedEventType,
            String expectedTopic,
            String expectedPayload
    ) {

        // IntegrationEvent
        Assertions.assertEquals(command.paymentRequestId().paymentRequestId(), integrationEvent.paymentRequestId());
        Assertions.assertEquals(command.bookingId().bookingId(), integrationEvent.bookingId());
        Assertions.assertEquals(payment.getPaymentId().paymentId(), integrationEvent.paymentId());
        Assertions.assertEquals(expectedStatus, integrationEvent.status());

        // El evento de salida tiene un UUID diferente al evento recibido
        Assertions.assertNotEquals(command.integrationEventId(), integrationEvent.integrationEventId());

        // OutboxEvent
        Assertions.assertEquals(expectedEventType, outboxEvent.eventType());
        Assertions.assertEquals(expectedTopic, outboxEvent.topic());
        Assertions.assertEquals(OutboxEventStatus.PENDING, outboxEvent.status());
        Assertions.assertEquals(command.bookingId().bookingId().toString(), outboxEvent.messageKey());

        Assertions.assertEquals(payment.getPaymentId().paymentId(), outboxEvent.aggregateId());
        Assertions.assertEquals(expectedPayload, outboxEvent.payload());
        Assertions.assertNull(outboxEvent.publishedAt());

        // Identidad compartida entre IntegrationEvent y OutboxEvent
        Assertions.assertEquals(integrationEvent.integrationEventId(), outboxEvent.id().outboxEventId());
    }
}