package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.util.Optional;

/**
 * Flags the first time a customer pays into a high-risk category.
 *
 * Plenty of people buy gift cards or use crypto exchanges legitimately, so we
 * don't flag the category itself. We only care when someone who has never done it
 * suddenly starts, because that change in behaviour is what fraud often looks like.
 */
public class HighRiskMerchantRule implements FraudRule {

    @Override
    public String name() {
        return "high-risk-merchant";
    }

    @Override
    public Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile) {
        MerchantCategory category = transaction.merchantCategory();
        if (!category.isHighRisk() || profile.knownCategories().contains(category)) {
            return Optional.empty();
        }
        return Optional.of(new RuleHit(name(), 15,
                "First payment to a high-risk category (" + category.label() + ")"));
    }
}
