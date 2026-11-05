package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence;

import com.rubenmarin.paymentservice.domain.model.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public class PaymentMapper {

    public static Payment toDomain(PaymentJpaEntity paymentJpaEntity) {

        PaymentId paymentId = new PaymentId(paymentJpaEntity.getPaymentId());
        PaymentStatus paymentStatus = paymentJpaEntity.getPaymentStatus();
        BookingId bookingId = new BookingId(paymentJpaEntity.getBookingId());
        PaymentRequestId paymentRequestId = new PaymentRequestId(paymentJpaEntity.getPaymentRequestId());
        Money money = new Money(paymentJpaEntity.getMoneyAmount(), Currency.getInstance(paymentJpaEntity.getMoneyCurrency()));

        return Payment.rehydrate(paymentId, paymentStatus, bookingId, paymentRequestId, money);
    }

    public static PaymentJpaEntity toEntity(Payment payment) {

        UUID paymentId = payment.getPaymentId().paymentId();
        PaymentStatus paymentStatus = payment.getPaymentStatus();
        UUID bookingId = payment.getBookingId().bookingId();
        UUID paymentRequestId = payment.getPaymentRequestId().paymentRequestId();
        BigDecimal moneyAmount = payment.getMoney().amount();
        String moneyCurrency = payment.getMoney().currency().getCurrencyCode();

        return new PaymentJpaEntity(
                paymentId,
                paymentStatus,
                bookingId,
                paymentRequestId,
                moneyAmount,
                moneyCurrency);
    }
}
