package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.Formatting;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.util.Optional;

/**
 * Flags a payment in the middle of the night from someone who never pays at night.
 *
 * This is the weakest of our rules, so it only adds a few points. Night owls exist,
 * which is exactly why we check the customer's own history first.
 */
public class LateNightRule implements FraudRule {

    @Override
    public String name() {
        return "late-night";
    }

    @Override
    public Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile) {
        boolean night = CustomerProfile.isNight(transaction.occurredAt());
        if (!night || !profile.hasEstablishedHistory() || profile.nightTransactionCount() > 0) {
            return Optional.empty();
        }
        return Optional.of(new RuleHit(name(), 10,
                "Payment at " + Formatting.timeOfDay(transaction.occurredAt())
                        + "; this customer has never paid at night before"));
    }
}
