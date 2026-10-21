package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A summary of how a customer normally behaves, built from their past payments.
 *
 * This is the heart of the whole idea. A £900 payment is not suspicious on its
 * own. It is suspicious when this particular customer usually spends £25. So
 * before judging a new payment, we look back at what is "normal" for them.
 *
 * A profile is always built "as of" a moment in time, and only uses payments that
 * happened before that moment. That matters: if we let the profile peek at the
 * future, our accuracy numbers would look much better than they really are.
 */
public final class CustomerProfile {

    /** How far back we look. Older behaviour is less useful. */
    public static final Duration LOOKBACK = Duration.ofDays(90);

    /** The window we use to spot bursts of payments. */
    public static final Duration BURST_WINDOW = Duration.ofMinutes(10);

    /** Below this many past payments, we don't really know the customer yet. */
    public static final int MIN_HISTORY = 5;

    private final int transactionCount;
    private final BigDecimal typicalAmount;
    private final Set<String> knownCountries;
    private final Set<MerchantCategory> knownCategories;
    private final int transactionsInBurstWindow;
    private final int nightTransactionCount;
    private final Duration timeSinceLastTransaction;

    private CustomerProfile(int transactionCount,
                            BigDecimal typicalAmount,
                            Set<String> knownCountries,
                            Set<MerchantCategory> knownCategories,
                            int transactionsInBurstWindow,
                            int nightTransactionCount,
                            Duration timeSinceLastTransaction) {
        this.transactionCount = transactionCount;
        this.typicalAmount = typicalAmount;
        this.knownCountries = Collections.unmodifiableSet(knownCountries);
        this.knownCategories = Collections.unmodifiableSet(knownCategories);
        this.transactionsInBurstWindow = transactionsInBurstWindow;
        this.nightTransactionCount = nightTransactionCount;
        this.timeSinceLastTransaction = timeSinceLastTransaction;
    }

    /**
     * Builds a profile from a customer's history, as it looked at {@code asOf}.
     *
     * @param history the customer's earlier payments, in any order. Anything at or
     *                after {@code asOf}, or older than {@link #LOOKBACK}, is ignored.
     * @param asOf    the moment of the payment we are about to judge
     */
    public static CustomerProfile fromHistory(List<TransactionDetails> history, Instant asOf) {
        Instant oldestUseful = asOf.minus(LOOKBACK);
        Instant burstStart = asOf.minus(BURST_WINDOW);

        List<BigDecimal> amounts = new ArrayList<>();
        Set<String> countries = new TreeSet<>();
        Set<MerchantCategory> categories = EnumSet.noneOf(MerchantCategory.class);
        int inBurstWindow = 0;
        int atNight = 0;
        Instant mostRecent = null;

        for (TransactionDetails past : history) {
            Instant when = past.occurredAt();
            boolean usable = when.isBefore(asOf) && !when.isBefore(oldestUseful);
            if (!usable) {
                continue;
            }

            amounts.add(past.amount());
            countries.add(past.country());
            categories.add(past.merchantCategory());

            if (!when.isBefore(burstStart)) {
                inBurstWindow++;
            }
            if (isNight(when)) {
                atNight++;
            }
            if (mostRecent == null || when.isAfter(mostRecent)) {
                mostRecent = when;
            }
        }

        Duration sinceLast = mostRecent == null ? null : Duration.between(mostRecent, asOf);
        return new CustomerProfile(amounts.size(), median(amounts), countries, categories,
                inBurstWindow, atNight, sinceLast);
    }

    /** A profile for someone we have never seen before. */
    public static CustomerProfile empty() {
        return fromHistory(List.of(), Instant.EPOCH);
    }

    /**
     * "Night" here means midnight to 5am UTC.
     * A real bank would use the customer's own time zone; we keep it simple.
     */
    public static boolean isNight(Instant instant) {
        int hour = instant.atZone(ZoneOffset.UTC).getHour();
        return hour < 5;
    }

    /**
     * We use the median rather than the average on purpose. If someone buys a
     * laptop once, the average jumps a lot but the median barely moves, so one
     * big purchase doesn't make every later purchase look "normal".
     */
    private static BigDecimal median(List<BigDecimal> amounts) {
        if (amounts.isEmpty()) {
            return null;
        }
        List<BigDecimal> sorted = new ArrayList<>(amounts);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /** True once we have seen enough payments to say what "normal" looks like. */
    public boolean hasEstablishedHistory() {
        return transactionCount >= MIN_HISTORY;
    }

    /**
     * How many times bigger this amount is than the customer's typical payment.
     * Returns 1.0 (meaning "nothing unusual") when we have no history to compare with.
     */
    public double spendRatio(BigDecimal amount) {
        if (typicalAmount == null || typicalAmount.signum() == 0) {
            return 1.0;
        }
        return amount.doubleValue() / typicalAmount.doubleValue();
    }

    public int transactionCount() {
        return transactionCount;
    }

    /** The median past payment, or null if there is no history. */
    public BigDecimal typicalAmount() {
        return typicalAmount;
    }

    public Set<String> knownCountries() {
        return knownCountries;
    }

    public Set<MerchantCategory> knownCategories() {
        return knownCategories;
    }

    /** Payments in the {@link #BURST_WINDOW} just before this one. */
    public int transactionsInBurstWindow() {
        return transactionsInBurstWindow;
    }

    public int nightTransactionCount() {
        return nightTransactionCount;
    }

    /** Time since the previous payment, or null if this is their first one. */
    public Duration timeSinceLastTransaction() {
        return timeSinceLastTransaction;
    }
}
