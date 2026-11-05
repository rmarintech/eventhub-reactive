package com.rubenmarin.paymentservice.application.port.in;

import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;

public interface CreatePaymentUseCase {

    Payment create(CreatePaymentCommand createPaymentCommand) ;
}
