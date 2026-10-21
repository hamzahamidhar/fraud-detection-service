package io.github.hamzahamidhar.fraud.domain;

/**
 * What we decided to do with a payment.
 *
 * Banks rarely just say "fraud" or "not fraud". Blocking a genuine customer's
 * card is annoying and costs money too, so there is usually a middle option where
 * a person (or a text message to the customer) double-checks before anything
 * drastic happens. That middle option is REVIEW.
 */
public enum Decision {

    /** Looks normal. Let it through. */
    APPROVE,

    /** Something is a bit off. Let a human or the customer confirm it. */
    REVIEW,

    /** Very likely fraud. Stop the payment. */
    BLOCK;

    /** True for anything we would want a person to look at. */
    public boolean isFlagged() {
        return this != APPROVE;
    }
}
