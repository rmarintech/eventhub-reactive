package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestCommand;
import com.rubenmarin.paymentservice.application.port.in.ProcessPaymentRequestUseCase;
import com.rubenmarin.paymentservice.application.port.out.InboxRepository;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;
import com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox.SpringDataInboxRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class ProcessPaymentRequestServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private ProcessPaymentRequestUseCase processPaymentRequestUseCase;

    @Autowired
    private SpringDataInboxRepository springDataInboxRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @Test
    void shouldRollbackInboxAndPaymentWhenOutboxFails() {

        // GIVEN
        UUID integrationEventId = UUID.randomUUID();

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money = new Money(new BigDecimal("150.00"), Currency.getInstance("EUR"));

        ProcessPaymentRequestCommand command = new ProcessPaymentRequestCommand(integrationEventId, bookingId, paymentRequestId, money);

        // Simulamos un error técnico al persistir el Outbox.
        doThrow(new RuntimeException("Simulated Outbox failure")).when(outboxRepository).save(any());

        // WHEN
        RuntimeException exception = Assertions.assertThrows(
                RuntimeException.class,
                () -> processPaymentRequestUseCase.processPaymentRequest(command)
        );

        // THEN

        // 1. Comprobar que el fallo se propaga
        Assertions.assertEquals("Simulated Outbox failure", exception.getMessage());

        // 2. Verificar que se intentó guardar el Outbox
        verify(outboxRepository).save(any());

        // 3. Verificar que el Inbox se ha revertido
        Assertions.assertFalse(springDataInboxRepository.existsById(integrationEventId), "Inbox event should have been rolled back");

        // 4. Verificar que el Payment se ha revertido
        Assertions.assertTrue(paymentRepository.findByPaymentRequestId(paymentRequestId).isEmpty(), "Payment should have been rolled back");
    }

}