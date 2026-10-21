package io.github.hamzahamidhar.fraud.config;

import io.github.hamzahamidhar.fraud.service.TransactionService;
import io.github.hamzahamidhar.fraud.simulation.LabelledTransaction;
import io.github.hamzahamidhar.fraud.simulation.SyntheticDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fills the database with a couple of months of made-up payments when the app starts,
 * so the dashboard has something to show straight away.
 *
 * Every payment goes through the real {@link TransactionService}, in time order, so
 * the stored scores are exactly what the live service would have produced.
 *
 * It uses a different seed from the training data, so these customers are new to
 * the model. Turn it off with fraud.demo-data.enabled=false (the tests do this).
 */
@Component
@ConditionalOnProperty(name = "fraud.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final int DAYS = 60;
    private static final long DEMO_SEED = 7;

    private final TransactionService service;
    private final int customers;

    public DemoDataLoader(TransactionService service,
                          @Value("${fraud.demo-data.customers:40}") int customers) {
        this.service = service;
        this.customers = customers;
    }

    @Override
    public void run(String... args) {
        Instant now = service.now();
        Instant start = now.minus(Duration.ofDays(DAYS)).truncatedTo(ChronoUnit.DAYS);

        // Skip anything the generator placed in the future (it can run a day or two over).
        List<LabelledTransaction> payments = new SyntheticDataGenerator(DEMO_SEED)
                .generate(customers, start, DAYS).stream()
                .filter(p -> p.details().occurredAt().isBefore(now))
                .toList();

        long flagged = payments.stream()
                .map(p -> service.process(p.details()))
                .filter(saved -> saved.getDecision().isFlagged())
                .count();

        log.info("Loaded {} demo payments for {} customers; {} were flagged for review or blocked.",
                payments.size(), customers, flagged);
        log.info("Dashboard ready at http://localhost:8080");
    }
}
