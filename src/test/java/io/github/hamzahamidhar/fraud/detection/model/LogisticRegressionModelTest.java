package io.github.hamzahamidhar.fraud.detection.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticRegressionModelTest {

    private static final String[] NAMES = {"signal", "noise"};

    /**
     * A toy problem with a known answer: fraud happens exactly when the first
     * feature is high, and the second feature is random noise. A working model
     * should learn a big weight for the first feature and ignore the second.
     */
    @Test
    void learnsWhichFeatureMatters() {
        Random random = new Random(1);
        List<double[]> rows = new ArrayList<>();
        List<Boolean> labels = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            double signal = random.nextDouble();
            rows.add(new double[]{signal, random.nextDouble()});
            labels.add(signal > 0.8);
        }

        LogisticRegressionModel model = LogisticRegressionModel.train(
                rows, labels, NAMES, LogisticRegressionModel.TrainingOptions.defaults());

        assertTrue(model.predictProbability(new double[]{0.95, 0.5}) > 0.8, "clearly fraud");
        assertTrue(model.predictProbability(new double[]{0.10, 0.5}) < 0.2, "clearly fine");
        assertEquals("signal", model.strongestReason(new double[]{0.95, 0.5}));

        double signalWeight = model.weightsByFeature().get("signal");
        double noiseWeight = model.weightsByFeature().get("noise");
        assertTrue(signalWeight > 5 * Math.abs(noiseWeight), "the signal should matter far more than the noise");
    }

    @Test
    void refusesToTrainWithoutAnyFraudExamples() {
        List<double[]> rows = List.of(new double[]{1, 2}, new double[]{3, 4});
        List<Boolean> labels = List.of(false, false);

        assertThrows(IllegalArgumentException.class, () -> LogisticRegressionModel.train(
                rows, labels, NAMES, LogisticRegressionModel.TrainingOptions.defaults()));
    }

    @Test
    void sigmoidSquashesNumbersIntoZeroToOne() {
        assertEquals(0.5, LogisticRegressionModel.sigmoid(0), 1e-9);
        assertTrue(LogisticRegressionModel.sigmoid(10) > 0.99);
        assertTrue(LogisticRegressionModel.sigmoid(-10) < 0.01);
    }
}
