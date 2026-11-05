package com.rubenmarin.paymentservice.application.port.in;

import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;

public record CreatePaymentCommand(
        BookingId bookingId,
        PaymentRequestId paymentRequestId,
        Money money
) {

    public CreatePaymentCommand {
        if (paymentRequestId == null) throw new IllegalArgumentException("paymentRequestId is null");
        if (money == null) throw new IllegalArgumentException("money is null");
        if (bookingId == null) throw new IllegalArgumentException("bookingId is null");
    }

}
