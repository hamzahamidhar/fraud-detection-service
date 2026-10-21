package io.github.hamzahamidhar.fraud.evaluation;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.simulation.TransactionLabel;

/**
 * One payment, ready to be judged: the payment itself, what the customer looked
 * like just before it, and the true answer.
 */
public record Example(TransactionDetails transaction, CustomerProfile profile, TransactionLabel label) {

    public boolean isFraud() {
        return label.isFraud();
    }
}
