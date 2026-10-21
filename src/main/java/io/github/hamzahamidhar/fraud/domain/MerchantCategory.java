package io.github.hamzahamidhar.fraud.domain;

/**
 * The kind of shop or service a payment went to.
 *
 * Real card networks use hundreds of "merchant category codes". We only need a
 * handful to make the detection interesting.
 *
 * A few categories are marked as high risk. Fraudsters like them because the
 * money is easy to turn into cash and hard to get back: gift cards can be resold,
 * crypto can be moved on in minutes, and gambling winnings can be withdrawn.
 */
public enum MerchantCategory {

    GROCERIES("Groceries", false),
    RESTAURANTS("Restaurants", false),
    TRANSPORT("Transport", false),
    ONLINE_SHOPPING("Online shopping", false),
    ELECTRONICS("Electronics", false),
    TRAVEL("Travel", false),
    ENTERTAINMENT("Entertainment", false),
    GAMBLING("Gambling", true),
    CRYPTO("Crypto exchange", true),
    GIFT_CARDS("Gift cards", true);

    private final String label;
    private final boolean highRisk;

    MerchantCategory(String label, boolean highRisk) {
        this.label = label;
        this.highRisk = highRisk;
    }

    /** A friendly name we can show to people, e.g. "Gift cards" instead of GIFT_CARDS. */
    public String label() {
        return label;
    }

    public boolean isHighRisk() {
        return highRisk;
    }
}
