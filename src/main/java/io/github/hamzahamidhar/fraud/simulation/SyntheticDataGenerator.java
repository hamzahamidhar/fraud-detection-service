package io.github.hamzahamidhar.fraud.simulation;

import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.domain.MerchantCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Makes up realistic-looking card payments, with some fraud planted in them.
 *
 * Why fake data? Real bank transactions are private, and for good reason. Public
 * fraud datasets exist, but most have their columns anonymised into meaningless
 * numbers, which makes it impossible to write rules a person can understand.
 * Generating our own data means we know exactly what is in it.
 *
 * The honest catch: a detector can only be as good as the data is realistic. If we
 * made fraud too obvious, we would get amazing scores that mean nothing. So the
 * generator deliberately includes genuine behaviour that LOOKS suspicious (holidays
 * abroad, one-off big purchases, quick bursts of small payments, night owls, regular
 * gamblers) and fraud that looks quite normal (the "subtle spend" pattern).
 *
 * Everything is driven by a seeded {@link Random}, so the same seed always produces
 * exactly the same data. That makes our results repeatable.
 */
public class SyntheticDataGenerator {

    private static final String[] FOREIGN_COUNTRIES = {"IE", "FR", "DE", "ES", "IN", "US", "NL", "AE"};
    private static final MerchantCategory[] EVERYDAY = {
            MerchantCategory.GROCERIES, MerchantCategory.RESTAURANTS, MerchantCategory.TRANSPORT,
            MerchantCategory.ONLINE_SHOPPING, MerchantCategory.ENTERTAINMENT
    };

    private final Random random;

    public SyntheticDataGenerator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * @param customerCount how many customers to invent
     * @param start         when the first day begins
     * @param days          how many days of activity to generate
     * @return every payment, sorted from oldest to newest
     */
    public List<LabelledTransaction> generate(int customerCount, Instant start, int days) {
        if (days < 30) {
            throw new IllegalArgumentException("Use at least 30 days so customers can build up some history");
        }
        List<LabelledTransaction> all = new ArrayList<>();
        for (int i = 1; i <= customerCount; i++) {
            all.addAll(generateCustomer(String.format("C%04d", i), start, days));
        }
        all.sort(Comparator.comparing(t -> t.details().occurredAt()));
        return all;
    }

    private List<LabelledTransaction> generateCustomer(String customerId, Instant start, int days) {
        // --- Who is this person? ---
        String home = random.nextDouble() < 0.85 ? "GB" : pick(FOREIGN_COUNTRIES);
        double typicalSpend = Math.max(3.0, Math.exp(Math.log(30) + 0.6 * random.nextGaussian()));
        boolean nightOwl = random.nextDouble() < 0.10;

        List<MerchantCategory> usualPlaces = new ArrayList<>();
        int placeCount = 3 + random.nextInt(2);
        while (usualPlaces.size() < placeCount) {
            MerchantCategory candidate = pick(EVERYDAY);
            if (!usualPlaces.contains(candidate)) {
                usualPlaces.add(candidate);
            }
        }
        if (random.nextDouble() < 0.10) {
            usualPlaces.add(MerchantCategory.GAMBLING); // Some people genuinely like a flutter.
        }
        if (random.nextDouble() < 0.05) {
            usualPlaces.add(MerchantCategory.CRYPTO);
        }

        List<LabelledTransaction> result = new ArrayList<>();

        // --- Their everyday spending ---
        int everydayCount = 20 + random.nextInt(41);
        for (int i = 0; i < everydayCount; i++) {
            int day = random.nextInt(days);
            int hour = nightOwl && random.nextDouble() < 0.3 ? random.nextInt(5) : 7 + random.nextInt(16);
            Instant when = at(start, day, hour);
            double amount = typicalSpend * Math.exp(0.5 * random.nextGaussian());
            result.add(genuine(customerId, amount, pick(usualPlaces), home, when));
        }

        // --- Genuine things that might look odd ---
        String abroad = pickOtherThan(home);

        if (random.nextDouble() < 0.15) { // A holiday
            int day = 10 + random.nextInt(days - 15);
            int payments = 3 + random.nextInt(4);
            for (int i = 0; i < payments; i++) {
                Instant when = at(start, day + random.nextInt(3), 8 + random.nextInt(14));
                MerchantCategory where = pick(new MerchantCategory[]{
                        MerchantCategory.RESTAURANTS, MerchantCategory.TRANSPORT, MerchantCategory.TRAVEL});
                double amount = typicalSpend * Math.exp(0.2 + 0.5 * random.nextGaussian());
                result.add(genuine(customerId, amount, where, abroad, when));
            }
        }

        if (random.nextDouble() < 0.20) { // A one-off big purchase, like a laptop or flights
            Instant when = at(start, 5 + random.nextInt(days - 5), 9 + random.nextInt(12));
            double amount = typicalSpend * (4 + random.nextDouble() * 11);
            MerchantCategory where = random.nextBoolean() ? MerchantCategory.ELECTRONICS : MerchantCategory.TRAVEL;
            result.add(genuine(customerId, amount, where, home, when));
        }

        if (random.nextDouble() < 0.08) { // A quick burst, like a day out at a shopping centre
            Instant when = at(start, 5 + random.nextInt(days - 5), 10 + random.nextInt(10));
            int payments = 4 + random.nextInt(3);
            for (int i = 0; i < payments; i++) {
                when = when.plusSeconds(40 + random.nextInt(80));
                double amount = typicalSpend * 0.5 * Math.exp(0.4 * random.nextGaussian());
                MerchantCategory where = pick(new MerchantCategory[]{
                        MerchantCategory.RESTAURANTS, MerchantCategory.ENTERTAINMENT, MerchantCategory.GROCERIES});
                result.add(genuine(customerId, amount, where, home, when));
            }
        }

        // --- And sometimes, fraud ---
        if (random.nextDouble() < 0.25) {
            int day = 25 + random.nextInt(days - 25); // Late enough that there is history to compare with.
            double roll = random.nextDouble();
            if (roll < 0.35) {
                result.addAll(cardTesting(customerId, home, abroad, start, day));
            } else if (roll < 0.75) {
                result.addAll(accountTakeover(customerId, home, abroad, typicalSpend, start, day));
            } else {
                result.addAll(subtleSpend(customerId, home, typicalSpend, start, day));
            }
        }

        return result;
    }

    private List<LabelledTransaction> cardTesting(String customerId, String home, String abroad,
                                                  Instant start, int day) {
        List<LabelledTransaction> fraud = new ArrayList<>();
        String country = random.nextBoolean() ? home : abroad;
        Instant when = at(start, day, random.nextInt(24));

        int tinyPayments = 4 + random.nextInt(4);
        for (int i = 0; i < tinyPayments; i++) {
            when = when.plusSeconds(30 + random.nextInt(60));
            double amount = 0.5 + random.nextDouble() * 2.5;
            fraud.add(fraudulent(customerId, amount, MerchantCategory.ONLINE_SHOPPING, country, when,
                    TransactionLabel.CARD_TESTING));
        }
        when = when.plusSeconds(60);
        MerchantCategory bigOne = random.nextBoolean() ? MerchantCategory.ELECTRONICS : MerchantCategory.GIFT_CARDS;
        fraud.add(fraudulent(customerId, 200 + random.nextDouble() * 1000, bigOne, country, when,
                TransactionLabel.CARD_TESTING));
        return fraud;
    }

    private List<LabelledTransaction> accountTakeover(String customerId, String home, String abroad,
                                                      double typicalSpend, Instant start, int day) {
        List<LabelledTransaction> fraud = new ArrayList<>();
        String country = random.nextDouble() < 0.7 ? abroad : home;
        int hour = random.nextDouble() < 0.4 ? random.nextInt(5) : 8 + random.nextInt(14);
        Instant when = at(start, day, hour);

        int payments = 1 + random.nextInt(3);
        for (int i = 0; i < payments; i++) {
            MerchantCategory where = pick(new MerchantCategory[]{
                    MerchantCategory.ELECTRONICS, MerchantCategory.CRYPTO,
                    MerchantCategory.GIFT_CARDS, MerchantCategory.TRAVEL});
            double amount = typicalSpend * (6 + random.nextDouble() * 24);
            fraud.add(fraudulent(customerId, amount, where, country, when, TransactionLabel.ACCOUNT_TAKEOVER));
            when = when.plus(Duration.ofMinutes(30 + random.nextInt(150)));
        }
        return fraud;
    }

    private List<LabelledTransaction> subtleSpend(String customerId, String home, double typicalSpend,
                                                  Instant start, int day) {
        List<LabelledTransaction> fraud = new ArrayList<>();
        int payments = 1 + random.nextInt(2);
        for (int i = 0; i < payments; i++) {
            Instant when = at(start, day + i, 9 + random.nextInt(12));
            MerchantCategory where = pick(new MerchantCategory[]{
                    MerchantCategory.ONLINE_SHOPPING, MerchantCategory.GIFT_CARDS, MerchantCategory.ELECTRONICS});
            double amount = typicalSpend * (1.5 + random.nextDouble() * 2.5);
            fraud.add(fraudulent(customerId, amount, where, home, when, TransactionLabel.SUBTLE_SPEND));
        }
        return fraud;
    }

    // --- Small helpers ---

    private Instant at(Instant start, int day, int hour) {
        return start.plus(Duration.ofDays(day))
                .plus(Duration.ofHours(hour))
                .plusSeconds(random.nextInt(3600));
    }

    private static LabelledTransaction genuine(String customerId, double amount, MerchantCategory category,
                                               String country, Instant when) {
        return new LabelledTransaction(details(customerId, amount, category, country, when),
                TransactionLabel.LEGITIMATE);
    }

    private static LabelledTransaction fraudulent(String customerId, double amount, MerchantCategory category,
                                             String country, Instant when, TransactionLabel label) {
        return new LabelledTransaction(details(customerId, amount, category, country, when), label);
    }

    private static TransactionDetails details(String customerId, double amount, MerchantCategory category,
                                              String country, Instant when) {
        BigDecimal money = BigDecimal.valueOf(Math.max(0.5, amount)).setScale(2, RoundingMode.HALF_UP);
        return new TransactionDetails(customerId, money, category, country, when);
    }

    private <T> T pick(T[] options) {
        return options[random.nextInt(options.length)];
    }

    private <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    private String pickOtherThan(String country) {
        String choice;
        do {
            choice = pick(FOREIGN_COUNTRIES);
        } while (choice.equals(country));
        return choice;
    }
}
