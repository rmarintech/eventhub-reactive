package com.rubenmarin.eventhub.event.model;

import com.rubenmarin.eventhub.event.domain.model.Money;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

public class MoneyTest {

    @Test
    void shouldCreateMoneyInEuros() {
        Money money = Money.euros(new BigDecimal("10.99"));

        Assertions.assertEquals(Currency.getInstance("EUR"), money.currency());
        Assertions.assertEquals(new BigDecimal("10.99"), money.amount());


    }

    @Test
    void shouldRejectNegativeAmount() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Money.euros(
                        new BigDecimal("-1.00")
                )
        );
    }

    @Test
    void shouldAllowZeroAmount() {
        Money money = Money.euros(BigDecimal.ZERO);

        Assertions.assertEquals(BigDecimal.ZERO, money.amount());
    }
}
