package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Shortcuts for building payments in tests, so each test reads like a story. */
public final class TestPayments {

    /** A Monday afternoon. Any fixed moment works; fixed means the tests never flicker. */
    public static final Instant NOON = Instant.parse("2026-03-02T12:00:00Z");

    private TestPayments() {
    }

    public static TransactionDetails payment(String amount, MerchantCategory category, String country, Instant when) {
        return new TransactionDetails("C1", new BigDecimal(amount), category, country, when);
    }

    /** A customer who has paid £20 for groceries in GB once a day for {@code days} days. */
    public static List<TransactionDetails> steadyGroceryShopper(int days, Instant before) {
        List<TransactionDetails> history = new ArrayList<>();
        for (int day = days; day >= 1; day--) {
            history.add(payment("20.00", MerchantCategory.GROCERIES, "GB", before.minus(Duration.ofDays(day))));
        }
        return history;
    }

    /** That same shopper's profile, as it looks at {@code asOf}. */
    public static CustomerProfile steadyProfile(Instant asOf) {
        return CustomerProfile.fromHistory(steadyGroceryShopper(10, asOf), asOf);
    }
}
