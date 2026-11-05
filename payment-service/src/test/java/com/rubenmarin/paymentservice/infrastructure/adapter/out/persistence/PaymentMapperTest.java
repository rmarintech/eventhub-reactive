package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence;

import com.rubenmarin.paymentservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

class PaymentMapperTest {

    /**
     * 	Conversión Domain → JPA
     * */
    @Test
    void shouldMapPaymentToJpaEntity() {

        // GIVEN: crear un Payment con sus identificadores e importe.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment payment = Payment.create(bookingId, paymentRequestId, money);

        // WHEN: convertir el agregado de dominio en entidad JPA.
        PaymentJpaEntity entity = PaymentMapper.toEntity(payment);

        // THEN: verificar que todos los campos se han convertido correctamente.
        Assertions.assertEquals(payment.getPaymentId().paymentId(), entity.getPaymentId());
        Assertions.assertEquals(payment.getPaymentStatus(), entity.getPaymentStatus());
        Assertions.assertEquals(bookingId.bookingId(), entity.getBookingId());
        Assertions.assertEquals(paymentRequestId.paymentRequestId(), entity.getPaymentRequestId());
        Assertions.assertEquals(money.amount(), entity.getMoneyAmount());
        Assertions.assertEquals(money.currency().getCurrencyCode(), entity.getMoneyCurrency());
    }

    /**
     * 	Conversión JPA → Domain
     * */
    @Test
    void shouldMapJpaEntityToPayment() {

        // GIVEN: crear un Payment y convertirlo en entidad JPA.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("99.95"), Currency.getInstance("EUR"));

        Payment original = Payment.create(bookingId, paymentRequestId, money);
        PaymentJpaEntity entity = PaymentMapper.toEntity(original);

        // WHEN: reconstruir el agregado desde la entidad JPA.
        Payment restored = PaymentMapper.toDomain(entity);

        // THEN: comprobar que conserva todos los valores.
        Assertions.assertEquals(original.getPaymentId(), restored.getPaymentId());
        Assertions.assertEquals(original.getPaymentStatus(), restored.getPaymentStatus());
        Assertions.assertEquals(original.getBookingId(), restored.getBookingId());
        Assertions.assertEquals(original.getPaymentRequestId(), restored.getPaymentRequestId());
        Assertions.assertEquals(original.getMoney(), restored.getMoney());
    }

    /**
     * Conservación de datos e identidad en ambas conversiones
     */
    @Test
    void shouldPreservePaymentDataAfterRoundTrip() {

        // GIVEN: crear un Payment y completar su procesamiento.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("125.50"), Currency.getInstance("USD"));

        Payment original = Payment.create(bookingId, paymentRequestId, money);
        original.markAsCompleted();

        // WHEN: realizar una conversión completa:
        // Dominio -> Entidad JPA -> Dominio.
        PaymentJpaEntity entity = PaymentMapper.toEntity(original);
        Payment restored = PaymentMapper.toDomain(entity);

        // THEN: la reconstrucción debe conservar la identidad.
        Assertions.assertEquals(original.getPaymentId(), restored.getPaymentId());
        Assertions.assertEquals(original.getBookingId(), restored.getBookingId());
        Assertions.assertEquals(original.getPaymentRequestId(), restored.getPaymentRequestId());

        // El estado COMPLETED debe conservarse.
        // No debe reinicializarse a PENDING durante la reconstrucción.
        Assertions.assertEquals(PaymentStatus.COMPLETED, restored.getPaymentStatus());

        // El importe y la divisa deben conservarse exactamente.
        Assertions.assertEquals(original.getMoney(), restored.getMoney());

        // El mapper crea una nueva instancia de dominio,
        // no devuelve la referencia original.
        Assertions.assertNotSame(original, restored);
    }
}