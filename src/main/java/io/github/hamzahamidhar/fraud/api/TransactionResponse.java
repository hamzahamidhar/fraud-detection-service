package io.github.hamzahamidhar.fraud.api;

import io.github.hamzahamidhar.fraud.domain.CardTransaction;
import io.github.hamzahamidhar.fraud.domain.Decision;
import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What we send back about a payment.
 *
 * We deliberately don't return the database entity directly. Having a separate
 * response type means we can change how things are stored without accidentally
 * changing what API users see, and we never leak a field we didn't mean to share.
 */
public record TransactionResponse(
        Long id,
        String customerId,
        BigDecimal amount,
        MerchantCategory merchantCategory,
        String country,
        Instant occurredAt,
        int riskScore,
        Decision decision,
        List<String> reasons) {

    public static TransactionResponse from(CardTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomerId(),
                transaction.getAmount(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getOccurredAt(),
                transaction.getRiskScore(),
                transaction.getDecision(),
                transaction.getReasons());
    }
}
