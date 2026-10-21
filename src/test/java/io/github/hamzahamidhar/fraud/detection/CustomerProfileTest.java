package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.MerchantCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.github.hamzahamidhar.fraud.detection.TestPayments.NOON;
import static io.github.hamzahamidhar.fraud.detection.TestPayments.payment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerProfileTest {

    @Test
    void aNewCustomerHasNoHistory() {
        CustomerProfile profile = CustomerProfile.fromHistory(List.of(), NOON);

        assertEquals(0, profile.transactionCount());
        assertFalse(profile.hasEstablishedHistory());
        assertNull(profile.typicalAmount());
        assertEquals(1.0, profile.spendRatio(new BigDecimal("500")), "no history means nothing to compare with");
    }

    @Test
    void typicalAmountIsTheMedianSoOneBigPurchaseDoesNotSkewIt() {
        List<TransactionDetails> history = new ArrayList<>(TestPayments.steadyGroceryShopper(6, NOON));
        history.add(payment("2000.00", MerchantCategory.ELECTRONICS, "GB", NOON.minus(Duration.ofHours(3))));

        CustomerProfile profile = CustomerProfile.fromHistory(history, NOON);

        assertEquals(new BigDecimal("20.00"), profile.typicalAmount());
    }

    @Test
    void onlyLooksAtThePastNeverTheFuture() {
        List<TransactionDetails> history = List.of(
                payment("10.00", MerchantCategory.GROCERIES, "GB", NOON.minus(Duration.ofDays(1))),
                payment("10.00", MerchantCategory.GROCERIES, "FR", NOON.plus(Duration.ofDays(1))));

        CustomerProfile profile = CustomerProfile.fromHistory(history, NOON);

        assertEquals(1, profile.transactionCount());
        assertFalse(profile.knownCountries().contains("FR"));
    }

    @Test
    void ignoresPaymentsOlderThanTheLookbackWindow() {
        Instant longAgo = NOON.minus(CustomerProfile.LOOKBACK).minus(Duration.ofDays(1));
        List<TransactionDetails> history = List.of(payment("10.00", MerchantCategory.GROCERIES, "GB", longAgo));

        assertEquals(0, CustomerProfile.fromHistory(history, NOON).transactionCount());
    }

    @Test
    void countsPaymentsInTheLastTenMinutes() {
        List<TransactionDetails> history = List.of(
                payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON.minus(Duration.ofMinutes(2))),
                payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON.minus(Duration.ofMinutes(5))),
                payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON.minus(Duration.ofMinutes(30))));

        CustomerProfile profile = CustomerProfile.fromHistory(history, NOON);

        assertEquals(2, profile.transactionsInBurstWindow());
        assertEquals(Duration.ofMinutes(2), profile.timeSinceLastTransaction());
    }

    @Test
    void nightMeansMidnightToFiveInTheMorningUtc() {
        assertTrue(CustomerProfile.isNight(Instant.parse("2026-03-02T03:30:00Z")));
        assertFalse(CustomerProfile.isNight(Instant.parse("2026-03-02T05:00:00Z")));
        assertFalse(CustomerProfile.isNight(NOON));
    }
}
