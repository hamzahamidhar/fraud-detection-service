package io.github.hamzahamidhar.fraud.detection.rules;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.TestPayments;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.domain.MerchantCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.hamzahamidhar.fraud.detection.TestPayments.NOON;
import static io.github.hamzahamidhar.fraud.detection.TestPayments.payment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each rule gets two kinds of test: one where it should speak up, and one where
 * it should stay quiet. The quiet cases matter just as much, because a rule that
 * fires on everything is useless.
 */
class FraudRulesTest {

    private final CustomerProfile steady = TestPayments.steadyProfile(NOON);
    private final CustomerProfile stranger = CustomerProfile.empty();

    @Nested
    class UnusualAmount {
        private final UnusualAmountRule rule = new UnusualAmountRule();

        @Test
        void quietForANormalAmount() {
            assertTrue(rule.check(payment("25.00", MerchantCategory.GROCERIES, "GB", NOON), steady).isEmpty());
        }

        @Test
        void mediumScoreForThreeTimesNormal() {
            Optional<RuleHit> hit = rule.check(payment("70.00", MerchantCategory.GROCERIES, "GB", NOON), steady);
            assertEquals(20, hit.orElseThrow().points());
        }

        @Test
        void highScoreAndAClearReasonForEightTimesNormal() {
            RuleHit hit = rule.check(payment("400.00", MerchantCategory.ELECTRONICS, "GB", NOON), steady).orElseThrow();
            assertEquals(40, hit.points());
            assertEquals("£400.00 is 20.0x this customer's typical payment of £20.00", hit.reason());
        }

        @Test
        void newCustomersAreOnlyFlaggedForReallyLargeAmounts() {
            assertTrue(rule.check(payment("300.00", MerchantCategory.ELECTRONICS, "GB", NOON), stranger).isEmpty());
            assertTrue(rule.check(payment("1500.00", MerchantCategory.ELECTRONICS, "GB", NOON), stranger).isPresent());
        }
    }

    @Nested
    class NewCountry {
        private final NewCountryRule rule = new NewCountryRule();

        @Test
        void quietAtHome() {
            assertTrue(rule.check(payment("20.00", MerchantCategory.GROCERIES, "GB", NOON), steady).isEmpty());
        }

        @Test
        void speaksUpForAFirstPaymentAbroad() {
            RuleHit hit = rule.check(payment("20.00", MerchantCategory.GROCERIES, "FR", NOON), steady).orElseThrow();
            assertTrue(hit.reason().contains("FR"));
        }

        @Test
        void quietForBrandNewCustomersBecauseEveryCountryIsNewToThem() {
            assertTrue(rule.check(payment("20.00", MerchantCategory.GROCERIES, "FR", NOON), stranger).isEmpty());
        }
    }

    @Nested
    class RapidFire {
        private final RapidFireRule rule = new RapidFireRule();

        private CustomerProfile afterQuickPayments(int count) {
            List<TransactionDetails> history = new ArrayList<>(TestPayments.steadyGroceryShopper(10, NOON));
            for (int i = 1; i <= count; i++) {
                history.add(payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON.minus(Duration.ofMinutes(i))));
            }
            return CustomerProfile.fromHistory(history, NOON);
        }

        @Test
        void quietForAFewPayments() {
            assertTrue(rule.check(payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON),
                    afterQuickPayments(3)).isEmpty());
        }

        @Test
        void speaksUpForTheFifthPaymentInTenMinutes() {
            RuleHit hit = rule.check(payment("1.00", MerchantCategory.ONLINE_SHOPPING, "GB", NOON),
                    afterQuickPayments(4)).orElseThrow();
            assertTrue(hit.reason().contains("number 5"));
        }
    }

    @Nested
    class HighRiskMerchant {
        private final HighRiskMerchantRule rule = new HighRiskMerchantRule();

        @Test
        void quietForEverydayShops() {
            assertTrue(rule.check(payment("20.00", MerchantCategory.RESTAURANTS, "GB", NOON), steady).isEmpty());
        }

        @Test
        void speaksUpTheFirstTimeSomeoneBuysGiftCards() {
            assertTrue(rule.check(payment("20.00", MerchantCategory.GIFT_CARDS, "GB", NOON), steady).isPresent());
        }

        @Test
        void quietForSomeoneWhoAlwaysBuysGiftCards() {
            List<TransactionDetails> history = new ArrayList<>(TestPayments.steadyGroceryShopper(10, NOON));
            history.add(payment("20.00", MerchantCategory.GIFT_CARDS, "GB", NOON.minus(Duration.ofDays(3))));
            CustomerProfile regular = CustomerProfile.fromHistory(history, NOON);

            assertTrue(rule.check(payment("20.00", MerchantCategory.GIFT_CARDS, "GB", NOON), regular).isEmpty());
        }
    }

    @Nested
    class LateNight {
        private final LateNightRule rule = new LateNightRule();
        private final Instant threeAm = Instant.parse("2026-03-02T03:00:00Z");

        @Test
        void quietDuringTheDay() {
            assertTrue(rule.check(payment("20.00", MerchantCategory.GROCERIES, "GB", NOON), steady).isEmpty());
        }

        @Test
        void speaksUpForADaytimeShopperPayingAtThreeAm() {
            // Their history is all at lunchtime on earlier days.
            CustomerProfile daytimeShopper = CustomerProfile.fromHistory(
                    TestPayments.steadyGroceryShopper(10, NOON), threeAm);
            assertTrue(rule.check(payment("20.00", MerchantCategory.GROCERIES, "GB", threeAm), daytimeShopper).isPresent());
        }
    }
}
