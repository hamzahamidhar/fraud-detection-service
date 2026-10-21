package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.util.Optional;

/**
 * Flags the first payment from a country the customer has never paid from.
 *
 * Stolen card details are often used abroad. On its own this rule is weak,
 * because people go on holiday all the time, so it only adds a moderate score.
 * It becomes convincing when it fires together with other rules.
 */
public class NewCountryRule implements FraudRule {

    @Override
    public String name() {
        return "new-country";
    }

    @Override
    public Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile) {
        if (!profile.hasEstablishedHistory()) {
            return Optional.empty(); // Everything is "new" for a new customer.
        }
        if (profile.knownCountries().contains(transaction.country())) {
            return Optional.empty();
        }
        String usual = String.join(", ", profile.knownCountries());
        return Optional.of(new RuleHit(name(), 25,
                "First payment from " + transaction.country()
                        + "; this customer has only paid from " + usual + " before"));
    }
}
