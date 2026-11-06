package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment;

import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

@SpringBootTest
@Testcontainers
class JpaPaymentRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldSaveAndRetrievePayment() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment payment = Payment.create(bookingId, paymentRequestId, money);
        Payment saved = paymentRepository.save(payment);
        Payment retrieved = paymentRepository.findById(saved.getPaymentId()).orElseThrow();

        Assertions.assertEquals(payment.getPaymentId(), retrieved.getPaymentId());
        Assertions.assertEquals(payment.getPaymentRequestId(), retrieved.getPaymentRequestId());
        Assertions.assertEquals(payment.getPaymentStatus(), retrieved.getPaymentStatus());
        Assertions.assertEquals(payment.getBookingId(), retrieved.getBookingId());
        Assertions.assertEquals(payment.getMoney().currency(), retrieved.getMoney().currency());
        Assertions.assertEquals(0, payment.getMoney().amount().compareTo(retrieved.getMoney().amount()));

    }

    @Test
    void shouldPersistCompletedPayment() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment payment = Payment.create(bookingId, paymentRequestId, money);
        Payment saved = paymentRepository.save(payment);
        Payment retrieved = paymentRepository.findById(payment.getPaymentId()).orElseThrow();

        Assertions.assertEquals(payment.getPaymentId(), retrieved.getPaymentId());
        Assertions.assertEquals(PaymentStatus.PENDING, retrieved.getPaymentStatus());

        retrieved.markAsCompleted();

        Payment savedAgain = paymentRepository.save(retrieved);
        Payment retrievedAgain = paymentRepository.findById(payment.getPaymentId()).orElseThrow();

        Assertions.assertEquals(payment.getPaymentId(), retrievedAgain.getPaymentId());
        Assertions.assertEquals(PaymentStatus.COMPLETED, retrievedAgain.getPaymentStatus());

    }

    @Test
    void shouldRejectDuplicatePaymentRequestId() {

        BookingId booking1Id = BookingId.generateBookingId();
        BookingId booking2Id = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money1 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));
        Money money2 = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment payment1 = Payment.create(booking1Id, paymentRequestId, money1);
        Payment payment2 = Payment.create(booking2Id, paymentRequestId, money2);

        Payment saved = paymentRepository.save(payment1);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.save(payment2);
        });

    }

    @Test
    void shouldPreserveMoneyAmountAndCurrency() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.7766"), Currency.getInstance("EUR"));

        Payment payment = Payment.create(bookingId, paymentRequestId, money);

        Payment saved = paymentRepository.save(payment);
        Payment retrieved = paymentRepository.findById(payment.getPaymentId()).orElseThrow();

        Assertions.assertEquals(payment.getPaymentId(), retrieved.getPaymentId());
        Assertions.assertEquals(money, retrieved.getMoney());
    }

    @Test
    void shouldFindPaymentByPaymentRequestId() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.7766"), Currency.getInstance("EUR"));

        Payment payment = Payment.create(bookingId, paymentRequestId, money);

        Payment saved = paymentRepository.save(payment);
        Payment retrieved = paymentRepository.findByPaymentRequestId(payment.getPaymentRequestId()).orElseThrow();

        Assertions.assertEquals(payment.getPaymentId(), retrieved.getPaymentId());
        Assertions.assertEquals(payment.getPaymentRequestId(), retrieved.getPaymentRequestId());
        Assertions.assertEquals(payment.getBookingId(), retrieved.getBookingId());
        Assertions.assertEquals(payment.getPaymentStatus(), retrieved.getPaymentStatus());
        Assertions.assertEquals(payment.getMoney(), retrieved.getMoney());
    }

    @Test
    void shouldReturnEmptyWhenPaymentRequestIdDoesNotExist() {
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Optional<Payment> retrieved = paymentRepository.findByPaymentRequestId(paymentRequestId);
        Assertions.assertTrue(retrieved.isEmpty());
    }
}
