package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.detection.model.FeatureExtractor;
import io.github.hamzahamidhar.fraud.detection.model.LogisticRegressionModel;
import io.github.hamzahamidhar.fraud.detection.rules.FraudRule;
import io.github.hamzahamidhar.fraud.detection.rules.HighRiskMerchantRule;
import io.github.hamzahamidhar.fraud.detection.rules.LateNightRule;
import io.github.hamzahamidhar.fraud.detection.rules.NewCountryRule;
import io.github.hamzahamidhar.fraud.detection.rules.RapidFireRule;
import io.github.hamzahamidhar.fraud.detection.rules.RuleHit;
import io.github.hamzahamidhar.fraud.detection.rules.UnusualAmountRule;
import io.github.hamzahamidhar.fraud.domain.Decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Brings the rules and the model together and decides what to do with a payment.
 *
 * Why use both? They are good at different things.
 *
 *   - Rules are predictable and easy to explain ("5 payments in 10 minutes").
 *     But they only catch what someone thought to write a rule for, and each
 *     threshold is a hard cliff: 7.9x normal spend scores very differently from 8x.
 *   - The model weighs everything at once and can pick up on combinations of
 *     small signals. But on its own it is harder to explain.
 *
 * So the final score is a blend of the two, and the reasons list always shows the
 * rules that fired, plus the model's main concern when it is worried.
 */
public class FraudDetector {

    /** How much the rules and the model each count towards the final score. */
    static final double RULE_WEIGHT = 0.5;
    static final double MODEL_WEIGHT = 0.5;

    /** Scores at or above these lines get reviewed or blocked. */
    public static final int REVIEW_THRESHOLD = 40;
    public static final int BLOCK_THRESHOLD = 70;

    private final List<FraudRule> rules;
    private final LogisticRegressionModel model;
    private final FeatureExtractor featureExtractor;

    public FraudDetector(List<FraudRule> rules, LogisticRegressionModel model, FeatureExtractor featureExtractor) {
        this.rules = List.copyOf(rules);
        this.model = model;
        this.featureExtractor = featureExtractor;
    }

    /** The standard set of rules this project ships with. */
    public static List<FraudRule> defaultRules() {
        return List.of(
                new UnusualAmountRule(),
                new NewCountryRule(),
                new RapidFireRule(),
                new HighRiskMerchantRule(),
                new LateNightRule());
    }

    public FraudAssessment assess(TransactionDetails transaction, CustomerProfile profile) {
        // 1. Ask every rule what it thinks.
        List<String> reasons = new ArrayList<>();
        int rulePoints = 0;
        for (FraudRule rule : rules) {
            Optional<RuleHit> hit = rule.check(transaction, profile);
            if (hit.isPresent()) {
                rulePoints += hit.get().points();
                reasons.add(hit.get().reason());
            }
        }
        int ruleScore = Math.min(100, rulePoints);

        // 2. Ask the model.
        double[] features = featureExtractor.extract(transaction, profile);
        double probability = model.predictProbability(features);
        if (probability >= 0.5) {
            // Capped at 99%: a model should never claim to be completely certain.
            long percent = Math.min(99, Math.round(probability * 100));
            reasons.add("The model rates this " + percent
                    + "% likely to be fraud, mainly because of: " + model.strongestReason(features));
        }

        // 3. Blend the two into one score and pick a decision.
        int riskScore = (int) Math.round(RULE_WEIGHT * ruleScore + MODEL_WEIGHT * probability * 100);
        return new FraudAssessment(riskScore, decide(riskScore), reasons, ruleScore, probability);
    }

    static Decision decide(int riskScore) {
        if (riskScore >= BLOCK_THRESHOLD) {
            return Decision.BLOCK;
        }
        if (riskScore >= REVIEW_THRESHOLD) {
            return Decision.REVIEW;
        }
        return Decision.APPROVE;
    }
}
