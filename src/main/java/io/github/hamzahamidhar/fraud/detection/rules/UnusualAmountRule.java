package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.Formatting;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Flags payments that are much bigger than what this customer normally spends.
 *
 * Fraudsters who get hold of a card usually want to take as much as they can,
 * as fast as they can, so a sudden jump in size is one of the strongest signals.
 */
public class UnusualAmountRule implements FraudRule {

    static final double HIGH_MULTIPLE = 3.0;
    static final double EXTREME_MULTIPLE = 8.0;
    static final BigDecimal NEW_CUSTOMER_LIMIT = new BigDecimal("1000");

    @Override
    public String name() {
        return "unusual-amount";
    }

    @Override
    public Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile) {
        BigDecimal amount = transaction.amount();

        // Brand new customers have no "normal" yet, so we fall back to a simple limit.
        if (!profile.hasEstablishedHistory()) {
            if (amount.compareTo(NEW_CUSTOMER_LIMIT) > 0) {
                return Optional.of(new RuleHit(name(), 20,
                        "Large payment of " + Formatting.money(amount)
                                + " from a customer with very little history"));
            }
            return Optional.empty();
        }

        double ratio = profile.spendRatio(amount);
        if (ratio < HIGH_MULTIPLE) {
            return Optional.empty();
        }

        int points = ratio >= EXTREME_MULTIPLE ? 40 : 20;
        String reason = Formatting.money(amount) + " is " + Formatting.oneDecimal(ratio)
                + "x this customer's typical payment of " + Formatting.money(profile.typicalAmount());
        return Optional.of(new RuleHit(name(), points, reason));
    }
}
