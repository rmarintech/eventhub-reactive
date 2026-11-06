package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.payment;

import com.rubenmarin.paymentservice.domain.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JpaPaymentRepositoryAdapterTest {

    @Mock
    private SpringDataPaymentRepository springDataPaymentRepository;

    @InjectMocks
    private JpaPaymentRepositoryAdapter jpaPaymentRepositoryAdapter;


    @Test
    void shouldFindPaymentById() {
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment originalPayment = Payment.create(bookingId, paymentRequestId, money);
        PaymentId paymentId = originalPayment.getPaymentId();

        Mockito.when(springDataPaymentRepository.findById(paymentId.paymentId()))
                .thenReturn(Optional.of(PaymentMapper.toEntity(originalPayment)));

        Optional<Payment> foundOpt = jpaPaymentRepositoryAdapter.findById(originalPayment.getPaymentId());

        Assertions.assertTrue(foundOpt.isPresent());
        Payment found = foundOpt.orElseThrow();

        Assertions.assertEquals(bookingId, found.getBookingId());
        Assertions.assertEquals(paymentRequestId, found.getPaymentRequestId());
        Assertions.assertEquals(money, found.getMoney());
        Assertions.assertEquals(paymentId, found.getPaymentId());
        Assertions.assertEquals(originalPayment.getPaymentStatus(), found.getPaymentStatus());

        Mockito.verify(springDataPaymentRepository).findById(paymentId.paymentId());

    }

    @Test
    void shouldReturnEmptyWhenPaymentDoesNotExist() {
        PaymentId paymentId = PaymentId.generatePaymentId();

        Mockito.when(springDataPaymentRepository.findById(paymentId.paymentId()))
                .thenReturn(Optional.empty());

        Optional<Payment> foundOpt = jpaPaymentRepositoryAdapter.findById(paymentId);

        Assertions.assertTrue(foundOpt.isEmpty());

        Mockito.verify(springDataPaymentRepository).findById(paymentId.paymentId());

    }

    @Test
    void shouldSavePaymentAndReturnPersistedDomain() {

        // GIVEN: crear un agregado Payment válido.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment originalPayment = Payment.create(bookingId, paymentRequestId, money);

        // Simular que Spring Data JPA devuelve la entidad persistida.
        // thenAnswer() devuelve el mismo argumento recibido por save().
        Mockito.when(springDataPaymentRepository.save(Mockito.any(PaymentJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: guardar el pago a través del adaptador.
        Payment savedPayment = jpaPaymentRepositoryAdapter.save(originalPayment);

        // THEN: capturar la entidad enviada al repositorio JPA.
        ArgumentCaptor<PaymentJpaEntity> captor = ArgumentCaptor.forClass(PaymentJpaEntity.class);

        Mockito.verify(springDataPaymentRepository).save(captor.capture());

        PaymentJpaEntity capturedEntity = captor.getValue();

        // Verificar que el mapper convirtió correctamente los identificadores.
        Assertions.assertEquals(originalPayment.getPaymentId().paymentId(), capturedEntity.getPaymentId());
        Assertions.assertEquals(bookingId.bookingId(), capturedEntity.getBookingId());
        Assertions.assertEquals(paymentRequestId.paymentRequestId(), capturedEntity.getPaymentRequestId());

        // Verificar el estado del pago.
        Assertions.assertEquals(PaymentStatus.PENDING, capturedEntity.getPaymentStatus());

        // Verificar importe y divisa.
        Assertions.assertEquals(money.amount(), capturedEntity.getMoneyAmount());
        Assertions.assertEquals(money.currency().getCurrencyCode(), capturedEntity.getMoneyCurrency());

        // Verificar que el adaptador devuelve un agregado de dominio válido.
        Assertions.assertNotNull(savedPayment);
        Assertions.assertEquals(originalPayment.getPaymentId(), savedPayment.getPaymentId());
        Assertions.assertEquals(bookingId, savedPayment.getBookingId());
        Assertions.assertEquals(paymentRequestId, savedPayment.getPaymentRequestId());
        Assertions.assertEquals(PaymentStatus.PENDING, savedPayment.getPaymentStatus());
        Assertions.assertEquals(money, savedPayment.getMoney());

        // El adaptador reconstruye un nuevo objeto de dominio
        // a partir de la entidad devuelta por Spring Data JPA.
        Assertions.assertNotSame(originalPayment, savedPayment);
    }

    @Test
    void shouldFindPaymentByPaymentRequestId() {
        // GIVEN: crear un agregado Payment válido.
        BookingId bookingId = BookingId.generateBookingId();
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();
        Money money = new Money(new BigDecimal("47.77"), Currency.getInstance("EUR"));

        Payment originalPayment = Payment.create(bookingId, paymentRequestId, money);

        PaymentId paymentId = originalPayment.getPaymentId();

        Mockito.when(springDataPaymentRepository.findByPaymentRequestId(paymentRequestId.paymentRequestId()))
                .thenReturn(Optional.of(PaymentMapper.toEntity(originalPayment)));

        // WHEN: recuperar el pago a través del adaptador.
        Payment foundPayment = jpaPaymentRepositoryAdapter.findByPaymentRequestId(paymentRequestId).orElseThrow();

        Assertions.assertEquals(paymentRequestId, foundPayment.getPaymentRequestId());
        Assertions.assertEquals(paymentId, foundPayment.getPaymentId());

        Mockito.verify(springDataPaymentRepository).findByPaymentRequestId(paymentRequestId.paymentRequestId());

    }

    @Test
    void shouldReturnEmptyWhenPaymentRequestIdDoesNotExist() {
        PaymentRequestId paymentRequestId = PaymentRequestId.generatePaymentRequestId();

        Mockito.when(springDataPaymentRepository
                        .findByPaymentRequestId(paymentRequestId.paymentRequestId()))
                .thenReturn(Optional.empty());

        Optional<Payment> foundPaymentOpt = jpaPaymentRepositoryAdapter.findByPaymentRequestId(paymentRequestId);

        Assertions.assertTrue(foundPaymentOpt.isEmpty());

        Mockito.verify(springDataPaymentRepository)
                .findByPaymentRequestId(paymentRequestId.paymentRequestId());
    }
}