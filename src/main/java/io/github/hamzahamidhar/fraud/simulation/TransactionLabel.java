package io.github.hamzahamidhar.fraud.simulation;

/**
 * The "true answer" attached to each synthetic payment.
 *
 * In real life a bank only finds out a payment was fraud weeks later, when the
 * customer complains. In our synthetic data we know straight away, because we
 * planted the fraud ourselves. We also record which kind of fraud it was, so we
 * can see which patterns the detector is good or bad at.
 */
public enum TransactionLabel {

    LEGITIMATE,

    /** Several tiny payments to check a stolen card works, then a big one. */
    CARD_TESTING,

    /** Someone takes over the account and makes large payments, often from abroad. */
    ACCOUNT_TAKEOVER,

    /** Quietly spending a bit more than usual, at home, in the daytime. Hard to catch. */
    SUBTLE_SPEND;

    public boolean isFraud() {
        return this != LEGITIMATE;
    }
}
