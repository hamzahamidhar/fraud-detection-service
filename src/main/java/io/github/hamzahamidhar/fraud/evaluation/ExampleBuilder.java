package io.github.hamzahamidhar.fraud.evaluation;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.simulation.LabelledTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays synthetic payments in time order, the way the live service would see them.
 *
 * For each payment we build the customer's profile from only the payments that
 * came BEFORE it. This mimics real life: when a payment arrives, the bank only
 * knows the past.
 */
public final class ExampleBuilder {

    private ExampleBuilder() {
    }

    public static List<Example> build(List<LabelledTransaction> transactions) {
        // Group each customer's payments together, keeping them in time order.
        Map<String, List<LabelledTransaction>> byCustomer = new LinkedHashMap<>();
        transactions.stream()
                .sorted(Comparator.comparing(t -> t.details().occurredAt()))
                .forEach(t -> byCustomer.computeIfAbsent(t.details().customerId(), id -> new ArrayList<>()).add(t));

        List<Example> examples = new ArrayList<>();
        for (List<LabelledTransaction> customerPayments : byCustomer.values()) {
            List<TransactionDetails> history = new ArrayList<>();
            for (LabelledTransaction payment : customerPayments) {
                TransactionDetails details = payment.details();
                CustomerProfile profile = CustomerProfile.fromHistory(history, details.occurredAt());
                examples.add(new Example(details, profile, payment.label()));
                history.add(details); // Only now does this payment become "the past".
            }
        }
        return examples;
    }
}
