package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.exception.PaymentIdempotencyConflictException;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CreatePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CreatePaymentService createPaymentService;

    @Test
    void shouldCreateAndPersistPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Mockito.when(paymentRepository.save(Mockito.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentCommand createPaymentCommand = new CreatePaymentCommand(bookingId, paymentRequestId, money1);
        Payment saved = createPaymentService.create(createPaymentCommand);

        Assertions.assertEquals(bookingId, saved.getBookingId());
        Assertions.assertEquals(paymentRequestId, saved.getPaymentRequestId());
        Assertions.assertEquals(money1, saved.getMoney());
        Assertions.assertNotNull(saved.getPaymentId());
        Assertions.assertEquals(PaymentStatus.PENDING, saved.getPaymentStatus());

        ArgumentCaptor<Payment> paymentArgumentCaptor = ArgumentCaptor.forClass(Payment.class);
        Mockito.verify(paymentRepository).save(paymentArgumentCaptor.capture());
        Payment capturedPayment = paymentArgumentCaptor.getValue();

        Assertions.assertEquals(bookingId, capturedPayment.getBookingId());
        Assertions.assertEquals(paymentRequestId, capturedPayment.getPaymentRequestId());
        Assertions.assertEquals(money1, capturedPayment.getMoney());
        Assertions.assertNotNull(capturedPayment.getPaymentId());
        Assertions.assertEquals(PaymentStatus.PENDING, capturedPayment.getPaymentStatus());

        Assertions.assertSame(capturedPayment, saved);

    }

    @Test
    void shouldReturnExistingPaymentWhenRequestIsDuplicated() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        CreatePaymentCommand createPaymentCommand = new CreatePaymentCommand(bookingId, paymentRequestId, money);

        Mockito.when(paymentRepository.save(Mockito.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment saved1 = createPaymentService.create(createPaymentCommand);

        Mockito.when(paymentRepository.findByPaymentRequestId(paymentRequestId))
                .thenReturn(Optional.of(saved1));

        Payment saved2 = createPaymentService.create(createPaymentCommand);

        Assertions.assertSame(saved1, saved2);
        Assertions.assertEquals(saved1.getPaymentId(), saved2.getPaymentId());

        Mockito.verify(paymentRepository, Mockito.times(1)).save(Mockito.any(Payment.class));
        Mockito.verify(paymentRepository, Mockito.times(2)).findByPaymentRequestId(paymentRequestId);

    }

    static Stream<Arguments> invalidPaymentRequests() {
        return Stream.of(
                Arguments.of("Different bookingId", true, false, false),
                Arguments.of("Different amount", false, true, false),
                Arguments.of("Different currency", false, false, true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPaymentRequests")
    void shouldRejectDuplicatedPaymentRequestWithDifferentData(String scenario,
                                                               boolean differentBookingId,
                                                               boolean differentAmount,
                                                               boolean differentCurrency) {
        BookingId bookingId1 = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        BigDecimal moneyAmount1 = new BigDecimal("47.77");
        Currency moneyCurrency1 = Currency.getInstance("EUR");
        Money money1 = new Money(moneyAmount1, moneyCurrency1);
        CreatePaymentCommand createPaymentCommand1 = new CreatePaymentCommand(bookingId1, paymentRequestId, money1);

        BookingId bookingId2 = bookingId1;
        BigDecimal moneyAmount2 = moneyAmount1;
        Currency moneyCurrency2 = moneyCurrency1;

        if (differentBookingId) {
            bookingId2 = BookingId.generateBookingId();
        }
        if (differentAmount) {
            moneyAmount2 = new BigDecimal("46.66");
        }
        if (differentCurrency) {
            moneyCurrency2 = Currency.getInstance("USD");
        }

        Money money2 = new Money(moneyAmount2, moneyCurrency2);
        CreatePaymentCommand createPaymentCommand2 = new CreatePaymentCommand(bookingId2, paymentRequestId, money2);

        Mockito.when(paymentRepository.save(Mockito.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment saved1 = createPaymentService.create(createPaymentCommand1);

        Mockito.when(paymentRepository.findByPaymentRequestId(paymentRequestId)).thenReturn(Optional.of(saved1));


        Assertions.assertThrows(PaymentIdempotencyConflictException.class, () -> {
            Payment saved2 = createPaymentService.create(createPaymentCommand2);
        });


        Mockito.verify(paymentRepository, Mockito.times(1)).save(Mockito.any(Payment.class));
        Mockito.verify(paymentRepository, Mockito.times(2)).findByPaymentRequestId(paymentRequestId);
    }

    @Test
    void shouldReturnExistingPaymentWhenAmountHasDifferentScale() {

        // GIVEN: una solicitud de pago con importe 47.77 EUR.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        CreatePaymentCommand command1 = new CreatePaymentCommand(bookingId, paymentRequestId, money1);

        // Simular que el repositorio devuelve el pago recibido.
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Primera solicitud: se crea y persiste el pago.
        Payment saved1 = createPaymentService.create(command1);

        // GIVEN: segunda solicitud con los mismos identificadores y divisa,
        // pero con una escala decimal diferente: 47.7700 EUR.
        Money money2 = new Money(new BigDecimal("47.7700"), Currency.getInstance("EUR"));

        CreatePaymentCommand command2 = new CreatePaymentCommand(bookingId, paymentRequestId, money2);

        // Simular que el repositorio encuentra el pago existente.
        Mockito.when(paymentRepository.findByPaymentRequestId(paymentRequestId))
                .thenReturn(Optional.of(saved1));

        // WHEN: procesamos nuevamente la solicitud.
        Payment saved2 = createPaymentService.create(command2);

        // THEN: debe devolver el mismo pago, sin crear uno nuevo.
        Assertions.assertSame(saved1, saved2);

        Assertions.assertEquals(saved1.getPaymentId(), saved2.getPaymentId());

        // Verificar que ambos importes son numéricamente equivalentes.
        Assertions.assertEquals(0, money1.amount().compareTo(money2.amount())
        );

        // El repositorio solo debe guardar el primer pago.
        Mockito.verify(paymentRepository, Mockito.times(1))
                .save(Mockito.any(Payment.class));

        // Ambas solicitudes deben consultar el PaymentRequestId.
        Mockito.verify(paymentRepository, Mockito.times(2))
                .findByPaymentRequestId(paymentRequestId);
    }
}
