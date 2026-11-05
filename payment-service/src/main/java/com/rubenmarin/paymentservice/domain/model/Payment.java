package com.rubenmarin.paymentservice.domain.model;

import java.util.Objects;

public class Payment {

    private final PaymentId paymentId;
    private PaymentStatus paymentStatus;
    private final BookingId bookingId;
    private final PaymentRequestId paymentRequestId;
    private final Money money;

    private Payment(PaymentId paymentId, PaymentStatus paymentStatus, BookingId bookingId, PaymentRequestId paymentRequestId, Money money) {
        this.paymentId = Objects.requireNonNull(paymentId, "Payment id must not be null");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "Payment status must not be null");
        this.bookingId = Objects.requireNonNull(bookingId, "Booking id must not be null");
        this.paymentRequestId = Objects.requireNonNull(paymentRequestId, "Payment request must not be null");
        this.money = Objects.requireNonNull(money, "Payment money must not be null");
    }

    public static Payment create(BookingId bookingId, PaymentRequestId paymentRequestId,  Money money) {
        PaymentId paymentId = PaymentId.generatePaymentId();
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        return new Payment(paymentId, paymentStatus, bookingId, paymentRequestId, money);
    }

    public static Payment rehydrate(PaymentId paymentId, PaymentStatus paymentStatus, BookingId bookingId, PaymentRequestId paymentRequestId, Money money) {
        return new Payment(paymentId, paymentStatus, bookingId, paymentRequestId, money);
    }

    public void markAsCompleted() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed");
        }
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    public void markAsFailed() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be failed");
        }
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public PaymentRequestId getPaymentRequestId() {
        return paymentRequestId;
    }
    public Money getMoney() {
        return money;
    }
}
