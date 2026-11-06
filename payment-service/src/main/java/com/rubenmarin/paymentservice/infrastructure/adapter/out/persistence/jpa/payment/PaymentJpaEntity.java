package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment;

import com.rubenmarin.paymentservice.domain.model.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID paymentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;
    @Column(nullable = false)
    private UUID bookingId;
    @Column(nullable = false, unique = true)
    private UUID paymentRequestId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal moneyAmount;
    @Column(nullable = false, length = 3)
    private String moneyCurrency;

    protected PaymentJpaEntity() {
    }

    public PaymentJpaEntity(
            UUID paymentId,
            PaymentStatus paymentStatus,
            UUID bookingId,
            UUID paymentRequestId,
            BigDecimal moneyAmount,
            String moneyCurrency
    ) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.bookingId = bookingId;
        this.paymentRequestId = paymentRequestId;
        this.moneyAmount = moneyAmount;
        this.moneyCurrency = moneyCurrency;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getPaymentRequestId() {
        return paymentRequestId;
    }

    public BigDecimal getMoneyAmount() {
        return moneyAmount;
    }

    public String getMoneyCurrency() {
        return moneyCurrency;
    }
}
