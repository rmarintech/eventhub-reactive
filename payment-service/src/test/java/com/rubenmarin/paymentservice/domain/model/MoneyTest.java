package com.rubenmarin.paymentservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithPositiveAmountAndCurrency() {

        // GIVEN: un importe positivo y una divisa válida.
        BigDecimal amount = new BigDecimal("100.00");
        Currency currency = Currency.getInstance("EUR");

        // WHEN: creamos el Value Object Money.
        Money money = new Money(amount, currency);

        // THEN: conserva el importe y la divisa.
        Assertions.assertEquals(amount, money.amount());
        Assertions.assertEquals(currency, money.currency());
    }

    @Test
    void shouldRejectNullAmount() {

        // GIVEN: un importe nulo.
        Currency currency = Currency.getInstance("EUR");

        // WHEN / THEN: la creación debe rechazarse.
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Money(null, currency));
    }

    @Test
    void shouldRejectZeroAmount() {

        // GIVEN: un importe igual a cero.
        BigDecimal amount = BigDecimal.ZERO;
        Currency currency = Currency.getInstance("EUR");

        // WHEN / THEN: un pago no puede tener importe cero.
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Money(amount, currency));
    }

    @Test
    void shouldRejectNegativeAmount() {

        // GIVEN: un importe negativo.
        BigDecimal amount = new BigDecimal("-10.50");
        Currency currency = Currency.getInstance("EUR");

        // WHEN / THEN: los importes negativos están prohibidos.
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Money(amount, currency));
    }

    @Test
    void shouldRejectNullCurrency() {

        // GIVEN: un importe válido, pero sin divisa.
        BigDecimal amount = new BigDecimal("25.00");

        // WHEN / THEN: la divisa es obligatoria.
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Money(amount, null));
    }

    @Test
    void shouldAllowDecimalAmount() {

        // GIVEN: un importe con decimales.
        BigDecimal amount = new BigDecimal("19.99");
        Currency currency = Currency.getInstance("EUR");

        // WHEN: creamos Money.
        Money money = new Money(amount, currency);

        // THEN: se conserva exactamente el importe.
        Assertions.assertEquals(new BigDecimal("19.99"), money.amount());

        Assertions.assertEquals(currency, money.currency());
    }
}