package com.rubenmarin.paymentservice.application.service;

import com.rubenmarin.paymentservice.application.exception.PaymentIdempotencyConflictException;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentCommand;
import com.rubenmarin.paymentservice.application.port.in.CreatePaymentUseCase;
import com.rubenmarin.paymentservice.application.port.out.PaymentRepository;
import com.rubenmarin.paymentservice.domain.model.BookingId;
import com.rubenmarin.paymentservice.domain.model.Money;
import com.rubenmarin.paymentservice.domain.model.Payment;
import com.rubenmarin.paymentservice.domain.model.PaymentRequestId;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@SpringBootTest
@Testcontainers
class CreatePaymentServiceIT {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentServiceIT.class);
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Test
    void shouldReturnExistingPaymentWhenRequestIsDuplicatedInDatabase() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        CreatePaymentCommand command1 = new CreatePaymentCommand(bookingId, paymentRequestId, money);

        Payment payment1 = createPaymentUseCase.create(command1);
        Payment payment2 = createPaymentUseCase.create(command1);

        Assertions.assertEquals(payment1.getPaymentId(), payment2.getPaymentId());
        Assertions.assertEquals(payment1.getBookingId(), payment2.getBookingId());
        Assertions.assertEquals(payment1.getMoney().currency(), payment2.getMoney().currency());
        Assertions.assertEquals(0, payment1.getMoney().amount().compareTo(payment2.getMoney().amount()));

        Payment persisted = paymentRepository.findByPaymentRequestId(paymentRequestId).orElseThrow();

        Assertions.assertEquals(payment1.getPaymentId(), persisted.getPaymentId());
    }

    @Test
    void shouldRejectDuplicatedPaymentRequestWithDifferentDataInDatabase() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        BookingId bookingId2 = BookingId.generateBookingId();

        CreatePaymentCommand command1 = new CreatePaymentCommand(bookingId, paymentRequestId, money);
        CreatePaymentCommand command2 = new CreatePaymentCommand(bookingId2, paymentRequestId, money);

        Payment payment1 = createPaymentUseCase.create(command1);

        Assertions.assertThrows(PaymentIdempotencyConflictException.class, () -> {
            Payment payment2 = createPaymentUseCase.create(command2);
        });

        Payment persisted = paymentRepository.findByPaymentRequestId(paymentRequestId).orElseThrow();

        Assertions.assertEquals(bookingId, persisted.getBookingId());
        Assertions.assertEquals(money.currency(), persisted.getMoney().currency());
        Assertions.assertEquals(0, money.amount().compareTo(persisted.getMoney().amount()));

    }

    @Disabled("""
    Limitación conocida: idempotencia en solicitudes concurrentes.

    Las solicitudes duplicadas secuenciales se gestionan correctamente
    utilizando PaymentRequestId como clave de idempotencia.

    Sin embargo, cuando dos solicitudes con el mismo PaymentRequestId llegan
    simultáneamente, ambas pueden ejecutar findByPaymentRequestId() antes
    de que se haya persistido ningún pago.

    En consecuencia, ambas intentan insertar un nuevo registro.
    PostgreSQL garantiza la integridad de los datos mediante una restricción
    UNIQUE sobre payment_request_id, permitiendo únicamente una inserción.

    La segunda solicitud puede fallar con DataIntegrityViolationException
    en lugar de recuperar y devolver el pago existente.

    No se generan pagos duplicados, pero la idempotencia concurrente
    no está completamente resuelta desde el punto de vista del cliente.

    Mejora pendiente:
    Capturar específicamente la violación de la restricción UNIQUE,
    recuperar el pago existente utilizando una transacción válida,
    comprobar que los datos de la solicitud coinciden con los originales
    y devolver el pago existente.

    Comportamiento esperado:
    Ambas solicitudes deben finalizar correctamente y devolver
    el mismo PaymentId.
    """)
    @Test
    void shouldPreventDuplicatePaymentsUnderConcurrency() {

        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        CreatePaymentCommand command = new CreatePaymentCommand(bookingId, paymentRequestId, money);

        List<Payment> successfulPayments = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        // Nota: Al utilizar try-with-resources, ExecutorService.close() espera a que terminen las tareas.
        // Si alguna queda bloqueada, el timeout de get() no garantiza por sí solo que el test termine inmediatamente.
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {

            CountDownLatch startLatch = new CountDownLatch(1);

            Callable<Payment> paymentCallable1 = () -> {
                startLatch.await();
                return createPaymentUseCase.create(command);
            };
            Callable<Payment> paymentCallable2 = () -> {
                startLatch.await();
                return createPaymentUseCase.create(command);
            };

            Future<Payment> future1 = executorService.submit(paymentCallable1);
            Future<Payment> future2 = executorService.submit(paymentCallable2);

            // Liberar los dos hilos después de enviar ambas tareas.
            startLatch.countDown();

            try {
                Payment payment1 = future1.get(10, TimeUnit.SECONDS);
                successfulPayments.add(payment1);

                //ExecutionException: la tarea ha fallado. Su causa contiene la excepción original, por ejemplo, DataIntegrityViolationException.
                //InterruptedException: el hilo que espera el resultado ha sido interrumpido. No representa un fallo de procesamiento del pago.
            } catch (ExecutionException e) {
                errors.add(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            try {
                Payment payment2 = future2.get(10, TimeUnit.SECONDS);
                successfulPayments.add(payment2);
            } catch (ExecutionException e) {
                errors.add(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

//        Assertions.assertFalse(successfulPayments.isEmpty());
//        Assertions.assertEquals(2, successfulPayments.size() + errors.size());

        Assertions.assertTrue(errors.isEmpty());
        Assertions.assertEquals(2, successfulPayments.size());

        Optional<Payment> persistedOpt = paymentRepository.findByPaymentRequestId(paymentRequestId);
        Assertions.assertTrue(persistedOpt.isPresent());

        Payment persisted = persistedOpt.get();
        for (Payment payment : successfulPayments) {
            Assertions.assertEquals(persisted.getPaymentId(), payment.getPaymentId());
        }
//        for (Throwable error : errors) {
//            Assertions.assertInstanceOf(DataIntegrityViolationException.class, error);
//        }
    }
}
