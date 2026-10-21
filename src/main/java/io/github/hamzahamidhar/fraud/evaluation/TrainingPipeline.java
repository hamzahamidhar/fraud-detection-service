package io.github.hamzahamidhar.fraud.evaluation;

import io.github.hamzahamidhar.fraud.detection.FraudAssessment;
import io.github.hamzahamidhar.fraud.detection.FraudDetector;
import io.github.hamzahamidhar.fraud.detection.model.FeatureExtractor;
import io.github.hamzahamidhar.fraud.detection.model.LogisticRegressionModel;
import io.github.hamzahamidhar.fraud.simulation.LabelledTransaction;
import io.github.hamzahamidhar.fraud.simulation.SyntheticDataGenerator;
import io.github.hamzahamidhar.fraud.simulation.TransactionLabel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * The whole "learn, then check our homework" process in one place.
 *
 *   1. Generate synthetic payments with known fraud in them.
 *   2. Split the CUSTOMERS (not the payments) into a training group and a test group.
 *   3. Train the model on the training group.
 *   4. Measure how well the detector does on the test group, which it has never seen.
 *
 * Step 2 is the important one. If we split individual payments at random, the same
 * customer's payments would end up on both sides, and the model would effectively
 * be tested on people it already knows. Splitting by customer gives an honest
 * answer to the real question: "how well does this work on a NEW customer?"
 */
public final class TrainingPipeline {

    /** A fixed start date, so the same seed always gives exactly the same results. */
    private static final Instant DATA_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final int DAYS = 90;
    private static final double TEST_FRACTION = 0.3;

    private TrainingPipeline() {
    }

    /** What you get back: a detector ready to use, plus an honest report on it. */
    public record TrainingResult(FraudDetector detector, EvaluationReport report) {
    }

    public static TrainingResult run(int customerCount, long seed) {
        // 1. Make the data and replay it in time order.
        List<LabelledTransaction> payments = new SyntheticDataGenerator(seed).generate(customerCount, DATA_START, DAYS);
        List<Example> examples = ExampleBuilder.build(payments);

        // 2. Split by customer.
        Set<String> testCustomers = pickTestCustomers(examples, seed);
        List<Example> training = new ArrayList<>();
        List<Example> test = new ArrayList<>();
        for (Example example : examples) {
            (testCustomers.contains(example.transaction().customerId()) ? test : training).add(example);
        }

        // 3. Train the model.
        FeatureExtractor features = new FeatureExtractor();
        List<double[]> rows = new ArrayList<>();
        List<Boolean> labels = new ArrayList<>();
        for (Example example : training) {
            rows.add(features.extract(example.transaction(), example.profile()));
            labels.add(example.isFraud());
        }
        LogisticRegressionModel model = LogisticRegressionModel.train(
                rows, labels, FeatureExtractor.FEATURE_NAMES, LogisticRegressionModel.TrainingOptions.defaults());
        FraudDetector detector = new FraudDetector(FraudDetector.defaultRules(), model, features);

        // 4. Check it on customers it has never seen.
        EvaluationReport report = evaluate(detector, model, training.size(), test);
        return new TrainingResult(detector, report);
    }

    private static Set<String> pickTestCustomers(List<Example> examples, long seed) {
        List<String> customers = new ArrayList<>(new TreeSet<>(
                examples.stream().map(e -> e.transaction().customerId()).toList()));
        Collections.shuffle(customers, new Random(seed));
        int testCount = (int) Math.round(customers.size() * TEST_FRACTION);
        return new TreeSet<>(customers.subList(0, testCount));
    }

    private static EvaluationReport evaluate(FraudDetector detector, LogisticRegressionModel model,
                                             int trainingSize, List<Example> test) {
        Counter rules = new Counter();
        Counter modelOnly = new Counter();
        Counter combined = new Counter();
        Map<TransactionLabel, int[]> perPattern = new EnumMap<>(TransactionLabel.class); // {caught, total}
        int fraudCount = 0;

        for (Example example : test) {
            FraudAssessment assessment = detector.assess(example.transaction(), example.profile());
            boolean fraud = example.isFraud();

            rules.add(fraud, assessment.ruleScore() >= FraudDetector.REVIEW_THRESHOLD);
            modelOnly.add(fraud, assessment.modelProbability() >= 0.5);
            combined.add(fraud, assessment.decision().isFlagged());

            if (fraud) {
                fraudCount++;
                int[] counts = perPattern.computeIfAbsent(example.label(), label -> new int[2]);
                counts[1]++;
                if (assessment.decision().isFlagged()) {
                    counts[0]++;
                }
            }
        }

        Map<String, Double> recallByPattern = new LinkedHashMap<>();
        perPattern.forEach((label, counts) ->
                recallByPattern.put(label.name(), Math.round(1000.0 * counts[0] / counts[1]) / 1000.0));

        return new EvaluationReport(trainingSize, test.size(), fraudCount,
                rules.toMetrics(), modelOnly.toMetrics(), combined.toMetrics(),
                recallByPattern, model.weightsByFeature());
    }

    /** Tallies up the four confusion-matrix counts as we go. */
    private static final class Counter {
        int tp;
        int fp;
        int tn;
        int fn;

        void add(boolean actuallyFraud, boolean flagged) {
            if (actuallyFraud && flagged) {
                tp++;
            } else if (!actuallyFraud && flagged) {
                fp++;
            } else if (!actuallyFraud) {
                tn++;
            } else {
                fn++;
            }
        }

        Metrics toMetrics() {
            return Metrics.of(tp, fp, tn, fn);
        }
    }
}
