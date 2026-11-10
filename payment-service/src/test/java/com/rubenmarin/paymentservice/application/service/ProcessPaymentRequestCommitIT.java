
package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;
import com.rubenmarin.paymentservice.domain.model.PaymentStatus;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox.SpringDataInboxRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class ProcessPaymentRequestCommitIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private ProcessPaymentRequestUseCase processPaymentRequestUseCase;

    @Autowired
    private SpringDataInboxRepository springDataInboxRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCommitInboxPaymentAndOutboxWhenProcessingSucceeds() {

        // GIVEN
        UUID integrationEventId = UUID.randomUUID();

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money = new Money(
                new BigDecimal("150.00"), Currency.getInstance("EUR"));

        ProcessPaymentRequestCommand command =
                new ProcessPaymentRequestCommand(
                        integrationEventId,
                        bookingId,
                        paymentRequestId,
                        money
                );

        // WHEN
        // El pago simulado tiene éxito porque el importe es > 100.
        processPaymentRequestUseCase.processPaymentRequest(command);

        // THEN: INBOX
        Assertions.assertTrue(springDataInboxRepository.existsById(integrationEventId));

        // THEN: PAYMENT
        Payment persistedPayment = paymentRepository
                .findByPaymentRequestId(paymentRequestId).orElseThrow();

        Assertions.assertEquals(PaymentStatus.COMPLETED, persistedPayment.getPaymentStatus());
        Assertions.assertEquals(bookingId, persistedPayment.getBookingId());
        Assertions.assertEquals(paymentRequestId, persistedPayment.getPaymentRequestId());
        Assertions.assertEquals(0, money.amount().compareTo(persistedPayment.getMoney().amount()));
        Assertions.assertEquals(money.currency(), persistedPayment.getMoney().currency());

        // THEN: OUTBOX
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();

        List<OutboxEvent> matchingEvents = pendingEvents.stream()
                .filter(event ->
                        event.aggregateId().equals(
                                persistedPayment.getPaymentId().paymentId()
                        )
                )
                .toList();

        Assertions.assertEquals(1, matchingEvents.size());

        OutboxEvent outboxEvent = matchingEvents.get(0);
        Assertions.assertEquals("PaymentCompleted", outboxEvent.eventType());
        Assertions.assertEquals("payment-completed", outboxEvent.topic());
        Assertions.assertEquals(OutboxEventStatus.PENDING, outboxEvent.status());
        Assertions.assertNull(outboxEvent.publishedAt());
        Assertions.assertEquals(bookingId.bookingId().toString(), outboxEvent.messageKey());

        // THEN: INTEGRATION EVENT (JSON)
        JsonNode payload = objectMapper.readTree(outboxEvent.payload());
        Assertions.assertEquals(outboxEvent.id().outboxEventId().toString(), payload.get("integrationEventId").asString());
        Assertions.assertEquals(paymentRequestId.paymentRequestId().toString(), payload.get("paymentRequestId").asString());
        Assertions.assertEquals(bookingId.bookingId().toString(), payload.get("bookingId").asString());
        Assertions.assertEquals(persistedPayment.getPaymentId().paymentId().toString(), payload.get("paymentId").asString());
        Assertions.assertEquals("COMPLETED", payload.get("status").asString());
    }


    @Test
    void shouldCommitFailedPaymentAndOutboxWhenProcessingIsRejected() {

        // GIVEN
        UUID integrationEventId = UUID.randomUUID();

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        ProcessPaymentRequestCommand command =
                new ProcessPaymentRequestCommand(
                        integrationEventId,
                        bookingId,
                        paymentRequestId,
                        money
                );

        // WHEN
        // El simulador rechaza pagos con importe <= 100.
        processPaymentRequestUseCase.processPaymentRequest(command);

        // THEN: INBOX
        Assertions.assertTrue(springDataInboxRepository.existsById(integrationEventId));

        // THEN: PAYMENT
        Payment persistedPayment = paymentRepository.findByPaymentRequestId(paymentRequestId)
                .orElseThrow();

        Assertions.assertEquals(PaymentStatus.FAILED, persistedPayment.getPaymentStatus());
        Assertions.assertEquals(bookingId, persistedPayment.getBookingId());
        Assertions.assertEquals(paymentRequestId, persistedPayment.getPaymentRequestId());
        Assertions.assertEquals(0, money.amount().compareTo(persistedPayment.getMoney().amount()));
        Assertions.assertEquals(money.currency(), persistedPayment.getMoney().currency());

        // THEN: OUTBOX
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();

        List<OutboxEvent> matchingEvents = pendingEvents.stream()
                .filter(event ->
                        event.aggregateId().equals(
                                persistedPayment.getPaymentId().paymentId()
                        )
                )
                .toList();

        Assertions.assertEquals(1, matchingEvents.size());

        OutboxEvent outboxEvent = matchingEvents.get(0);
        Assertions.assertEquals("PaymentFailed", outboxEvent.eventType());
        Assertions.assertEquals("payment-failed", outboxEvent.topic());
        Assertions.assertEquals(OutboxEventStatus.PENDING, outboxEvent.status());
        Assertions.assertNull(outboxEvent.publishedAt());
        Assertions.assertEquals(bookingId.bookingId().toString(), outboxEvent.messageKey());

        // THEN: INTEGRATION EVENT (JSON)
        JsonNode payload = objectMapper.readTree(outboxEvent.payload());

        Assertions.assertEquals(outboxEvent.id().outboxEventId().toString(), payload.get("integrationEventId").asString());
        Assertions.assertEquals(paymentRequestId.paymentRequestId().toString(), payload.get("paymentRequestId").asString());
        Assertions.assertEquals(bookingId.bookingId().toString(), payload.get("bookingId").asString());
        Assertions.assertEquals(persistedPayment.getPaymentId().paymentId().toString(), payload.get("paymentId").asString());
        Assertions.assertEquals("FAILED", payload.get("status").asString());
    }

}
