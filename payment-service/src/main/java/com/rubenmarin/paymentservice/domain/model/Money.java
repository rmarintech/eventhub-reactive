package com.rubenmarin.paymentservice.domain.model;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(
        BigDecimal amount,
        Currency currency
        ) {

    public Money{
        if  (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.signum() < 1) {
            throw new IllegalArgumentException("Amount cannot be 0 nor negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }
}
