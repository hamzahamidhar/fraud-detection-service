package io.github.hamzahamidhar.fraud.detection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small helpers for turning numbers into text a person would actually want to read.
 * Every reason the detector gives ends up in front of a human, so it should read
 * like a sentence, not like a debug log.
 */
public final class Formatting {

    private static final DateTimeFormatter TIME_OF_DAY =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private Formatting() {
        // Just a bag of static helpers, nothing to create.
    }

    /** 1234.5 becomes "£1,234.50". */
    public static String money(BigDecimal amount) {
        return String.format(Locale.UK, "£%,.2f", amount);
    }

    /** 9.4321 becomes "9.4". */
    public static String oneDecimal(double value) {
        return String.format(Locale.UK, "%.1f", value);
    }

    /** An instant becomes a clock time like "03:12 UTC". */
    public static String timeOfDay(Instant instant) {
        return TIME_OF_DAY.format(instant) + " UTC";
    }
}
