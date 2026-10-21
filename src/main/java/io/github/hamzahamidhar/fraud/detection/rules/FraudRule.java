package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.util.Optional;

/**
 * One check that looks for one kind of suspicious behaviour.
 *
 * Every rule follows the same shape: look at the payment and the customer's
 * profile, and either stay quiet or return a {@link RuleHit} explaining what it
 * noticed. Because they all share this interface, the detector can run a list of
 * rules without knowing anything about how each one works. Adding a new rule
 * means writing one new class, and nothing else has to change.
 */
public interface FraudRule {

    /** A short, stable name such as "unusual-amount". */
    String name();

    /**
     * @return a hit if this payment looks suspicious to this rule, otherwise empty
     */
    Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile);
}
