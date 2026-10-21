package io.github.hamzahamidhar.fraud.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationTest {

    @Test
    void metricsAreWorkedOutCorrectly() {
        // 8 fraud caught, 2 false alarms, 88 left alone correctly, 2 fraud missed.
        Metrics metrics = Metrics.of(8, 2, 88, 2);

        assertEquals(0.8, metrics.precision()); // 8 of the 10 flagged were fraud
        assertEquals(0.8, metrics.recall());    // 8 of the 10 frauds were caught
        assertEquals(0.8, metrics.f1());
        assertEquals(0.022, metrics.falsePositiveRate()); // 2 of 90 genuine payments flagged
    }

    @Test
    void metricsDoNotCrashWhenNothingWasFlagged() {
        Metrics metrics = Metrics.of(0, 0, 100, 0);

        assertEquals(0.0, metrics.precision());
        assertEquals(0.0, metrics.recall());
    }

    /**
     * A safety net: if a future change quietly makes the detector much worse,
     * this test fails. The bars are set a little below the real results on purpose,
     * so normal small variations don't break the build.
     */
    @Test
    void combinedDetectorCatchesMostFraudOnUnseenCustomers() {
        EvaluationReport report = TrainingPipeline.run(600, 42).report();

        assertTrue(report.testFraudPayments() > 50, "need enough fraud to measure anything");
        assertTrue(report.combined().recall() >= 0.75, "recall was " + report.combined().recall());
        assertTrue(report.combined().falsePositiveRate() <= 0.05,
                "false positive rate was " + report.combined().falsePositiveRate());
        assertTrue(report.combined().f1() >= report.rulesOnly().f1(), "combining should beat rules alone");
    }
}
