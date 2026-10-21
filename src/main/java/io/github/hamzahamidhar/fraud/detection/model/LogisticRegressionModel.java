package io.github.hamzahamidhar.fraud.detection.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A logistic regression model, written from scratch.
 *
 * We could have used a machine learning library, but writing it ourselves keeps it
 * small and means every line can be explained. The idea is simple:
 *
 *   1. Give each feature a weight. A positive weight means "more of this makes
 *      fraud more likely".
 *   2. Multiply each feature by its weight, add them up, and add a bias.
 *   3. Squash the total into a probability between 0 and 1 using the sigmoid
 *      function.
 *
 * Training means finding weights that make the probabilities match the labels in
 * the training data. We do that with gradient descent: start with all weights at
 * zero, measure how wrong we are, nudge every weight a little in the direction
 * that makes us less wrong, and repeat a few hundred times.
 *
 * The big advantage over fancier models is that the weights are readable. We can
 * say exactly which feature pushed a payment towards "fraud", and banks need to be
 * able to explain their decisions.
 */
public final class LogisticRegressionModel {

    private final String[] featureNames;
    private final double[] weights;
    private final double bias;
    private final double[] means;
    private final double[] standardDeviations;

    private LogisticRegressionModel(String[] featureNames, double[] weights, double bias,
                                    double[] means, double[] standardDeviations) {
        this.featureNames = featureNames.clone();
        this.weights = weights;
        this.bias = bias;
        this.means = means;
        this.standardDeviations = standardDeviations;
    }

    /** The settings for training. The defaults work well for this project. */
    public record TrainingOptions(int epochs, double learningRate, double l2Penalty) {
        public static TrainingOptions defaults() {
            return new TrainingOptions(800, 0.5, 0.001);
        }
    }

    /**
     * Learns weights from labelled examples.
     *
     * @param rows   one array of features per payment
     * @param labels true where the payment was fraud
     */
    public static LogisticRegressionModel train(List<double[]> rows, List<Boolean> labels,
                                                String[] featureNames, TrainingOptions options) {
        if (rows.isEmpty() || rows.size() != labels.size()) {
            throw new IllegalArgumentException("Need the same number of rows and labels, and at least one of each");
        }
        int n = rows.size();
        int featureCount = featureNames.length;

        // Step 1: standardise every feature to mean 0 and standard deviation 1.
        // Otherwise "minutes since last payment" (big numbers) would drown out
        // "new country" (just 0 or 1), and training would be slow and lopsided.
        double[] means = new double[featureCount];
        double[] stds = new double[featureCount];
        for (double[] row : rows) {
            for (int j = 0; j < featureCount; j++) {
                means[j] += row[j] / n;
            }
        }
        for (double[] row : rows) {
            for (int j = 0; j < featureCount; j++) {
                double diff = row[j] - means[j];
                stds[j] += diff * diff / n;
            }
        }
        for (int j = 0; j < featureCount; j++) {
            stds[j] = Math.sqrt(stds[j]);
            if (stds[j] < 1e-9) {
                stds[j] = 1.0; // A feature that never changes; avoid dividing by zero.
            }
        }

        double[][] x = new double[n][featureCount];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < featureCount; j++) {
                x[i][j] = (rows.get(i)[j] - means[j]) / stds[j];
            }
        }

        // Step 2: class weights. Fraud is rare (a few percent of payments), so a lazy
        // model could score well by always saying "not fraud". Giving each fraud
        // example more weight forces the model to take fraud seriously.
        int fraudCount = 0;
        for (boolean label : labels) {
            if (label) {
                fraudCount++;
            }
        }
        if (fraudCount == 0 || fraudCount == n) {
            throw new IllegalArgumentException("Training data needs both fraud and genuine examples");
        }
        double fraudWeight = n / (2.0 * fraudCount);
        double genuineWeight = n / (2.0 * (n - fraudCount));

        // Step 3: gradient descent.
        double[] w = new double[featureCount];
        double b = 0.0;
        double totalWeight = fraudWeight * fraudCount + genuineWeight * (n - fraudCount);

        for (int epoch = 0; epoch < options.epochs(); epoch++) {
            double[] gradient = new double[featureCount];
            double biasGradient = 0.0;

            for (int i = 0; i < n; i++) {
                boolean isFraud = labels.get(i);
                double target = isFraud ? 1.0 : 0.0;
                double sampleWeight = isFraud ? fraudWeight : genuineWeight;

                double error = sigmoid(dot(w, x[i]) + b) - target; // how wrong we were
                for (int j = 0; j < featureCount; j++) {
                    gradient[j] += sampleWeight * error * x[i][j];
                }
                biasGradient += sampleWeight * error;
            }

            for (int j = 0; j < featureCount; j++) {
                // The L2 penalty gently pulls weights towards zero, so no single
                // feature can grow huge just to fit a few odd examples.
                double step = gradient[j] / totalWeight + options.l2Penalty() * w[j];
                w[j] -= options.learningRate() * step;
            }
            b -= options.learningRate() * biasGradient / totalWeight;
        }

        return new LogisticRegressionModel(featureNames, w, b, means, stds);
    }

    /** The model's estimate that this payment is fraud, between 0 and 1. */
    public double predictProbability(double[] features) {
        return sigmoid(dot(weights, standardise(features)) + bias);
    }

    /**
     * Which feature pushed this payment most strongly towards "fraud"?
     * Each feature's contribution is simply weight times (standardised) value.
     */
    public String strongestReason(double[] features) {
        double[] z = standardise(features);
        int best = 0;
        for (int j = 1; j < z.length; j++) {
            if (weights[j] * z[j] > weights[best] * z[best]) {
                best = j;
            }
        }
        return featureNames[best];
    }

    /** The learned weight for each feature, in a readable form (handy for the README). */
    public Map<String, Double> weightsByFeature() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int j = 0; j < featureNames.length; j++) {
            result.put(featureNames[j], Math.round(weights[j] * 100.0) / 100.0);
        }
        return result;
    }

    private double[] standardise(double[] features) {
        if (features.length != weights.length) {
            throw new IllegalArgumentException("Expected " + weights.length + " features but got " + features.length);
        }
        double[] z = new double[features.length];
        for (int j = 0; j < features.length; j++) {
            z[j] = (features[j] - means[j]) / standardDeviations[j];
        }
        return z;
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int j = 0; j < a.length; j++) {
            sum += a[j] * b[j];
        }
        return sum;
    }

    /** Squashes any number into the range 0 to 1. Big positive means close to 1. */
    static double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }
}
