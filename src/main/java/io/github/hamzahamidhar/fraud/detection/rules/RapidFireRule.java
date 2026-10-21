package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

import java.util.Optional;

/**
 * Flags lots of payments in a short space of time.
 *
 * A classic trick is "card testing": a fraudster makes several tiny payments to
 * check the stolen card works, then makes one big one. Normal people rarely pay
 * for five separate things inside ten minutes.
 */
public class RapidFireRule implements FraudRule {

    /** How many earlier payments in the window it takes before we get worried. */
    static final int THRESHOLD = 4;

    @Override
    public String name() {
        return "rapid-fire";
    }

    @Override
    public Optional<RuleHit> check(TransactionDetails transaction, CustomerProfile profile) {
        int earlier = profile.transactionsInBurstWindow();
        if (earlier < THRESHOLD) {
            return Optional.empty();
        }
        long minutes = CustomerProfile.BURST_WINDOW.toMinutes();
        return Optional.of(new RuleHit(name(), 30,
                "This is payment number " + (earlier + 1) + " in the last " + minutes + " minutes"));
    }
}
