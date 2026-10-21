package io.github.hamzahamidhar.fraud.simulation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticDataGeneratorTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void theSameSeedAlwaysGivesTheSameData() {
        List<LabelledTransaction> first = new SyntheticDataGenerator(5).generate(50, START, 60);
        List<LabelledTransaction> second = new SyntheticDataGenerator(5).generate(50, START, 60);

        assertEquals(first, second);
    }

    @Test
    void paymentsComeOutInTimeOrder() {
        List<LabelledTransaction> data = new SyntheticDataGenerator(5).generate(50, START, 60);

        for (int i = 1; i < data.size(); i++) {
            assertFalse(data.get(i).details().occurredAt().isBefore(data.get(i - 1).details().occurredAt()));
        }
    }

    @Test
    void fraudIsPresentButRare() {
        List<LabelledTransaction> data = new SyntheticDataGenerator(5).generate(400, START, 90);
        long fraud = data.stream().filter(LabelledTransaction::isFraud).count();
        double rate = (double) fraud / data.size();

        assertTrue(rate > 0.005 && rate < 0.05, "fraud rate was " + rate);
    }
}
