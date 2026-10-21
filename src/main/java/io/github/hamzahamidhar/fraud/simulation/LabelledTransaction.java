package io.github.hamzahamidhar.fraud.simulation;

import io.github.hamzahamidhar.fraud.detection.TransactionDetails;

/** A synthetic payment together with the truth about whether it was fraud. */
public record LabelledTransaction(TransactionDetails details, TransactionLabel label) {

    public boolean isFraud() {
        return label.isFraud();
    }
}
