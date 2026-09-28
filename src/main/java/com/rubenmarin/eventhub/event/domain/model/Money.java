package com.rubenmarin.eventhub.event.domain.model;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Represents a monetary value.
 *
 * Money is a Value Object:
 * - immutable
 * - equality based on its values
 * - has no identity
 */
public record Money(
        BigDecimal amount,
        Currency currency
) {

    public Money {

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Amount cannot be null"
            );
        }

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative"
            );
        }

        if (currency == null) {
            throw new IllegalArgumentException(
                    "Currency cannot be null"
            );
        }
    }

    public static Money euros(BigDecimal amount) {
        return new Money(
                amount,
                Currency.getInstance("EUR")
        );
    }
}