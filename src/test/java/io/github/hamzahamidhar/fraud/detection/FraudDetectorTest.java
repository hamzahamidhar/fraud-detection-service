package io.github.hamzahamidhar.fraud.detection;

import io.github.hamzahamidhar.fraud.domain.Decision;
import io.github.hamzahamidhar.fraud.domain.MerchantCategory;
import io.github.hamzahamidhar.fraud.evaluation.TrainingPipeline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.github.hamzahamidhar.fraud.detection.TestPayments.NOON;
import static io.github.hamzahamidhar.fraud.detection.TestPayments.payment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FraudDetectorTest {

    private static FraudDetector detector;

    @BeforeAll
    static void trainOnce() {
        // A smaller training run than the real app uses, to keep the tests quick.
        detector = TrainingPipeline.run(300, 42).detector();
    }

    @Test
    void approvesAnOrdinaryPayment() {
        FraudAssessment result = detector.assess(
                payment("22.00", MerchantCategory.GROCERIES, "GB", NOON), TestPayments.steadyProfile(NOON));

        assertEquals(Decision.APPROVE, result.decision());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void blocksAHugePaymentAbroadAtNightToANewHighRiskMerchant() {
        Instant threeAm = Instant.parse("2026-03-03T03:00:00Z");
        CustomerProfile profile = CustomerProfile.fromHistory(TestPayments.steadyGroceryShopper(10, NOON), threeAm);

        FraudAssessment result = detector.assess(payment("950.00", MerchantCategory.GIFT_CARDS, "AE", threeAm), profile);

        assertEquals(Decision.BLOCK, result.decision());
        assertTrue(result.riskScore() >= FraudDetector.BLOCK_THRESHOLD);
        assertTrue(result.reasons().size() >= 3, "every rule that fired should explain itself");
    }

    @Test
    void scoreIsAlwaysBetweenZeroAndOneHundred() {
        FraudAssessment result = detector.assess(
                payment("99999.00", MerchantCategory.CRYPTO, "US", NOON), TestPayments.steadyProfile(NOON));

        assertTrue(result.riskScore() >= 0 && result.riskScore() <= 100);
        assertTrue(result.ruleScore() <= 100);
    }

    @Test
    void decisionThresholds() {
        assertEquals(Decision.APPROVE, FraudDetector.decide(39));
        assertEquals(Decision.REVIEW, FraudDetector.decide(40));
        assertEquals(Decision.REVIEW, FraudDetector.decide(69));
        assertEquals(Decision.BLOCK, FraudDetector.decide(70));
        assertFalse(Decision.APPROVE.isFlagged());
    }
}
