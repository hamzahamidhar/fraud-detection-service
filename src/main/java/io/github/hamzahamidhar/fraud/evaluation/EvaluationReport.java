package io.github.hamzahamidhar.fraud.evaluation;

import java.util.Map;

/**
 * The results of testing the detector on customers it never saw during training.
 *
 * We report three versions side by side (rules alone, the model alone, and both
 * combined) so anyone can see what each part actually adds.
 *
 * @param trainingPayments  how many payments the model learned from
 * @param testPayments      how many payments it was tested on
 * @param testFraudPayments how many of the test payments were fraud
 * @param rulesOnly         rules alone, flagging at a rule score of 40 or more
 * @param modelOnly         the model alone, flagging at a probability of 0.5 or more
 * @param combined          the real detector: anything it would REVIEW or BLOCK
 * @param recallByPattern   for each kind of fraud, how much the combined detector caught
 * @param modelWeights      what the model learned: bigger positive numbers point towards fraud
 */
public record EvaluationReport(
        int trainingPayments,
        int testPayments,
        int testFraudPayments,
        Metrics rulesOnly,
        Metrics modelOnly,
        Metrics combined,
        Map<String, Double> recallByPattern,
        Map<String, Double> modelWeights) {
}
