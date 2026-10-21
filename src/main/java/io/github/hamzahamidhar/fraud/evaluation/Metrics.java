package io.github.hamzahamidhar.fraud.evaluation;

/**
 * How well a detector did, counted against the true answers.
 *
 * The four counts (often drawn as a "confusion matrix"):
 *   - true positive:  fraud, and we flagged it        (good)
 *   - false positive: genuine, but we flagged it      (an annoyed customer)
 *   - true negative:  genuine, and we let it through  (good)
 *   - false negative: fraud, and we missed it         (lost money)
 *
 * Plain accuracy is misleading here. If only 4% of payments are fraud, a detector
 * that never flags anything is 96% "accurate" and completely useless. So we look at:
 *
 *   - precision: of the payments we flagged, how many were really fraud?
 *   - recall:    of all the real fraud, how much did we catch?
 *   - F1:        a single number balancing the two
 *   - false positive rate: of all genuine payments, how many did we bother?
 */
public record Metrics(
        int truePositives,
        int falsePositives,
        int trueNegatives,
        int falseNegatives,
        double precision,
        double recall,
        double f1,
        double falsePositiveRate) {

    public static Metrics of(int truePositives, int falsePositives, int trueNegatives, int falseNegatives) {
        double precision = ratio(truePositives, truePositives + falsePositives);
        double recall = ratio(truePositives, truePositives + falseNegatives);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double falsePositiveRate = ratio(falsePositives, falsePositives + trueNegatives);
        return new Metrics(truePositives, falsePositives, trueNegatives, falseNegatives,
                round(precision), round(recall), round(f1), round(falsePositiveRate));
    }

    private static double ratio(int top, int bottom) {
        return bottom == 0 ? 0.0 : (double) top / bottom;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
