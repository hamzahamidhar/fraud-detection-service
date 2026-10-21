package io.github.hamzahamidhar.fraud.detection.model;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.time.Duration;

/**
 * Turns a payment (plus the customer's profile) into a list of numbers.
 *
 * Machine learning models can't read "First payment from FR". They need numbers.
 * Each position in the array is one "feature", one measurable thing about the
 * payment. The model then learns how much each feature matters.
 *
 * The features mirror what the rules look at, but as smooth numbers instead of
 * yes/no thresholds. That lets the model pick up on combinations the rules miss,
 * like "a bit bigger than usual AND a gift card AND quite soon after the last one".
 */
public final class FeatureExtractor {

    /** Human-readable names, in the same order as the numbers in {@link #extract}. */
    public static final String[] FEATURE_NAMES = {
            "payment much larger than usual",
            "new country",
            "many payments in a short time",
            "high-risk merchant",
            "new type of merchant",
            "unusual time of night",
            "little or no history",
            "time since previous payment"
    };

    /** Used when there is no previous payment: "a long time ago", roughly 30 days. */
    private static final double NO_PREVIOUS_PAYMENT_MINUTES = 30 * 24 * 60;

    public double[] extract(TransactionDetails transaction, CustomerProfile profile) {
        boolean established = profile.hasEstablishedHistory();

        // log() squashes big ratios: 2x vs 4x matters about as much as 10x vs 20x.
        // We only care about payments being BIGGER than usual, so below 1x counts as 0.
        double spendRatio = Math.max(1.0, profile.spendRatio(transaction.amount()));
        double logSpendRatio = established ? Math.log(spendRatio) : 0.0;

        double newCountry = established && !profile.knownCountries().contains(transaction.country()) ? 1 : 0;
        double burst = Math.log1p(profile.transactionsInBurstWindow());
        double highRisk = transaction.merchantCategory().isHighRisk() ? 1 : 0;
        double newCategory = established && !profile.knownCategories().contains(transaction.merchantCategory()) ? 1 : 0;
        double oddHour = established
                && CustomerProfile.isNight(transaction.occurredAt())
                && profile.nightTransactionCount() == 0 ? 1 : 0;
        double thinHistory = established ? 0 : 1;

        Duration sinceLast = profile.timeSinceLastTransaction();
        double minutesSinceLast = sinceLast == null ? NO_PREVIOUS_PAYMENT_MINUTES : sinceLast.toMinutes();
        double logMinutesSinceLast = Math.log1p(minutesSinceLast);

        return new double[]{
                logSpendRatio, newCountry, burst, highRisk, newCategory, oddHour, thinHistory, logMinutesSinceLast
        };
    }
}
