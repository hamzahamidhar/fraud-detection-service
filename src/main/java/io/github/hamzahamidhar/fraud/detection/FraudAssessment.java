package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.Decision;

import java.util.List;

/**
 * The detector's verdict on one payment.
 *
 * @param riskScore        overall risk from 0 (fine) to 100 (almost certainly fraud)
 * @param decision         what to do with the payment
 * @param reasons          plain-English explanations, so a person can see why
 * @param ruleScore        the score from the rules alone (0 to 100)
 * @param modelProbability the model's fraud probability alone (0 to 1)
 */
public record FraudAssessment(
        int riskScore,
        Decision decision,
        List<String> reasons,
        int ruleScore,
        double modelProbability) {

    public FraudAssessment {
        reasons = List.copyOf(reasons);
    }
}
