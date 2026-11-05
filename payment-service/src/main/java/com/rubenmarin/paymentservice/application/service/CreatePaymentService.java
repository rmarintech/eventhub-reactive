package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.exception.PaymentIdempotencyConflictException;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class CreatePaymentService implements CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private static final Logger log = LoggerFactory.getLogger(CreatePaymentService.class);

    public CreatePaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment create(CreatePaymentCommand createPaymentCommand) {

        Optional<Payment> paymentOpt = paymentRepository.findByPaymentRequestId(createPaymentCommand.paymentRequestId());

        if (paymentOpt.isEmpty()) {
            log.info("Creating new Payment: {}", createPaymentCommand);
            Payment payment = Payment.create(
                    createPaymentCommand.bookingId(),
                    createPaymentCommand.paymentRequestId(),
                    createPaymentCommand.money()
            );
            return paymentRepository.save(payment);
        } else {
            Payment found = paymentOpt.get();
            if (found.getBookingId().equals(createPaymentCommand.bookingId())
                    && found.getMoney().currency().equals(createPaymentCommand.money().currency())
                    && 0 == found.getMoney().amount().compareTo(createPaymentCommand.money().amount())) {
                log.info("Payment already in system: {}", createPaymentCommand);
                return found;
            } else {
                log.error("Payment already in system with different values: {}", createPaymentCommand);
                throw new PaymentIdempotencyConflictException("Payment already in system with different values");
            }

        }
    }
}
