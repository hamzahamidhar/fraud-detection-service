package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * The facts about a single card payment that the detector looks at.
 *
 * This is deliberately a plain Java record with no database or web annotations.
 * Keeping the detection logic free of frameworks means we can test it with
 * ordinary unit tests, and reuse it for the synthetic training data too.
 *
 * Money is a {@link BigDecimal}, never a double. Doubles can't store values like
 * 0.10 exactly, and small rounding errors are not acceptable when the numbers are
 * someone's money.
 */
public record TransactionDetails(
        String customerId,
        BigDecimal amount,
        MerchantCategory merchantCategory,
        String country,
        Instant occurredAt) {

    public TransactionDetails {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(merchantCategory, "merchantCategory is required");
        Objects.requireNonNull(country, "country is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be more than zero");
        }
    }
}
