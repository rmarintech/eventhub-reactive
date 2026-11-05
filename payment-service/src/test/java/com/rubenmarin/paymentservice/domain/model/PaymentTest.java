package com.rubenmarin.paymentservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

class PaymentTest {

    @Test
    void shouldCreatePendingPaymentWithGeneratedId() {

        // GIVEN / WHEN: crear dos pagos independientes.
        BookingId booking1Id = BookingId.generateBookingId();
        BookingId booking2Id = BookingId.generateBookingId();
        PaymentRequestId paymentRequest1Id = PaymentRequestId.generatePaymentRequestId();
        PaymentRequestId paymentRequest2Id = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Money money2 = new Money(new BigDecimal("49.99"), Currency.getInstance("USD"));
        Payment payment1 = Payment.create(booking1Id, paymentRequest1Id,money1);
        Payment payment2 = Payment.create(booking2Id, paymentRequest2Id,money2);

        // THEN: comprobar que el pago se ha creado correctamente.
        Assertions.assertNotNull(payment1);

        // Verificar que se ha generado un identificador.
        Assertions.assertNotNull(payment1.getPaymentId());

        // Verificar que el estado inicial es PENDING.
        Assertions.assertEquals(PaymentStatus.PENDING, payment1.getPaymentStatus());

        // Verificar que cada pago tiene una identidad diferente.
        Assertions.assertNotEquals(payment1.getPaymentId(), payment2.getPaymentId());
    }

    @Test
    void shouldCompletePendingPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsCompleted();
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment1.getPaymentStatus());

    }

    @Test
    void shouldFailPendingPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsFailed();
        Assertions.assertEquals(PaymentStatus.FAILED, payment1.getPaymentStatus());
    }

    @Test
    void shouldRejectCompletingCompletedPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsCompleted();
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment1.getPaymentStatus());

        Assertions.assertThrows(IllegalStateException.class, () -> payment1.markAsCompleted());
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment1.getPaymentStatus());
    }

    @Test
    void shouldRejectFailingCompletedPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsCompleted();
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment1.getPaymentStatus());

        Assertions.assertThrows(IllegalStateException.class, () -> payment1.markAsFailed());
        Assertions.assertEquals(PaymentStatus.COMPLETED, payment1.getPaymentStatus());
    }

    @Test
    void shouldRejectCompletingFailedPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsFailed();
        Assertions.assertEquals(PaymentStatus.FAILED, payment1.getPaymentStatus());

        Assertions.assertThrows(IllegalStateException.class, () -> payment1.markAsCompleted());
        Assertions.assertEquals(PaymentStatus.FAILED, payment1.getPaymentStatus());
    }

    @Test
    void shouldRejectFailingFailedPayment() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsFailed();
        Assertions.assertEquals(PaymentStatus.FAILED, payment1.getPaymentStatus());

        Assertions.assertThrows(IllegalStateException.class, () -> payment1.markAsFailed());
        Assertions.assertEquals(PaymentStatus.FAILED, payment1.getPaymentStatus());
    }

    @Test
    void shouldCreatePaymentWithBookingIdAndPaymentRequestId() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);

        Assertions.assertEquals(bookingId, payment1.getBookingId());
        Assertions.assertEquals(paymentRequestId, payment1.getPaymentRequestId());
    }

    @Test
    void shouldRejectNullBookingId() {
        BookingId bookingId = null;
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Assertions.assertThrows(NullPointerException.class, () -> Payment.create(bookingId, paymentRequestId,money1));
    }

    @Test
    void shouldRejectNullPaymentRequestId() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = null;
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Assertions.assertThrows(NullPointerException.class, () -> Payment.create(bookingId, paymentRequestId,money1));
    }

    @Test
    void shouldRehydratePaymentPreservingIdentifiersAndStatus() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);
        payment1.markAsCompleted();

        Payment payment2 = Payment.rehydrate(
                payment1.getPaymentId(),
                payment1.getPaymentStatus(),
                payment1.getBookingId(),
                payment1.getPaymentRequestId(),
                payment1.getMoney()
        );

        Assertions.assertEquals(payment1.getPaymentId(), payment2.getPaymentId());
        Assertions.assertEquals(payment1.getPaymentRequestId(), payment2.getPaymentRequestId());
        Assertions.assertEquals(payment1.getPaymentStatus(), payment2.getPaymentStatus());
        Assertions.assertEquals(payment1.getBookingId(), payment2.getBookingId());
        Assertions.assertEquals(payment1.getMoney(), payment2.getMoney());

    }

    @Test
   void shouldCreatePaymentWithMoney(){
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Payment payment1 = Payment.create(bookingId, paymentRequestId,money1);

        Assertions.assertEquals(money1, payment1.getMoney());
    }
    @Test
    void  shouldRejectNullMoney(){
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = null;
        Assertions.assertThrows(NullPointerException.class, () -> Payment.create(bookingId, paymentRequestId,money1));
    }
}