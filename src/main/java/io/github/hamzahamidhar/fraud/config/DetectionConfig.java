package io.github.hamzahamidhar.fraud.config;

import io.github.hamzahamidhar.fraud.detection.FraudDetector;
import io.github.hamzahamidhar.fraud.evaluation.EvaluationReport;
import io.github.hamzahamidhar.fraud.evaluation.TrainingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Sets up the objects the rest of the app depends on.
 *
 * Each method marked @Bean creates one object that Spring then hands to any class
 * that asks for it in its constructor. This is "dependency injection": classes say
 * what they need, and Spring works out how to provide it.
 *
 * When the app starts, we train the model on synthetic data and test it straight
 * away. That takes a couple of seconds and means the service always starts with a
 * detector that has been freshly checked. A real bank would train offline and load
 * a saved model instead; see the README for why we keep it simple here.
 */
@Configuration
public class DetectionConfig {

    private static final Logger log = LoggerFactory.getLogger(DetectionConfig.class);

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TrainingPipeline.TrainingResult trainingResult(
            @Value("${fraud.training.customers:1000}") int customers,
            @Value("${fraud.training.seed:42}") long seed) {
        log.info("Training the fraud model on {} synthetic customers (seed {})...", customers, seed);
        TrainingPipeline.TrainingResult result = TrainingPipeline.run(customers, seed);

        EvaluationReport report = result.report();
        log.info("Tested on {} payments from unseen customers ({} of them fraud).",
                report.testPayments(), report.testFraudPayments());
        log.info("Combined detector: precision {}, recall {}, false positive rate {}",
                report.combined().precision(), report.combined().recall(), report.combined().falsePositiveRate());
        return result;
    }

    @Bean
    public FraudDetector fraudDetector(TrainingPipeline.TrainingResult trainingResult) {
        return trainingResult.detector();
    }

    @Bean
    public EvaluationReport evaluationReport(TrainingPipeline.TrainingResult trainingResult) {
        return trainingResult.report();
    }
}
