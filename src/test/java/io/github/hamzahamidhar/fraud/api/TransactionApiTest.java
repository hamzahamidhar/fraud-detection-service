package io.github.hamzahamidhar.fraud.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the whole app the way a real client would use it: HTTP requests in,
 * JSON out, with the real database and detector in between.
 *
 * MockMvc sends requests straight into Spring without opening a network port,
 * which keeps these tests fast. Each test uses its own customer IDs so they
 * don't trip over each other's data.
 */
@SpringBootTest(properties = {
        "fraud.demo-data.enabled=false",   // start with an empty database
        "fraud.training.customers=300"     // a quicker training run for tests
})
@AutoConfigureMockMvc
class TransactionApiTest {

    private static final Instant MONDAY_NOON = Instant.parse("2026-03-02T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    private ResultActions submit(String customerId, String amount, String category, String country, Instant when)
            throws Exception {
        String json = """
                {"customerId": "%s", "amount": %s, "merchantCategory": "%s", "country": "%s", "occurredAt": "%s"}
                """.formatted(customerId, amount, category, country, when);
        return mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void anOrdinaryPaymentIsApprovedAndSaved() throws Exception {
        submit("API-1", "18.40", "GROCERIES", "GB", MONDAY_NOON)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerId").value("API-1"))
                .andExpect(jsonPath("$.decision").value("APPROVE"));

        mockMvc.perform(get("/api/customers/API-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void aSuddenBigPaymentAbroadIsFlaggedOnceTheCustomerHasHistory() throws Exception {
        // Ten ordinary days of £20 grocery shopping at home...
        for (int day = 10; day >= 1; day--) {
            submit("API-2", "20.00", "GROCERIES", "GB", MONDAY_NOON.minus(Duration.ofDays(day)))
                    .andExpect(status().isCreated());
        }

        // ...then £900 of gift cards from another country at 3am.
        Instant threeAm = MONDAY_NOON.minus(Duration.ofHours(9));
        submit("API-2", "900.00", "GIFT_CARDS", "AE", threeAm)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.reasons", hasItem("First payment to a high-risk category (Gift cards)")));

        mockMvc.perform(get("/api/transactions/flagged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].customerId", hasItem("API-2")));
    }

    @Test
    void badInputGetsAClearErrorMessage() throws Exception {
        submit("API-3", "-5", "GROCERIES", "gb", MONDAY_NOON)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.details", hasItem("amount must be more than zero")))
                .andExpect(jsonPath("$.details", hasItem("country must be a two-letter country code such as GB")));
    }

    @Test
    void anUnknownMerchantCategoryIsRejected() throws Exception {
        submit("API-4", "10.00", "SPACESHIPS", "GB", MONDAY_NOON)
                .andExpect(status().isBadRequest());
    }

    @Test
    void askingForAPaymentThatDoesNotExistGivesA404() throws Exception {
        mockMvc.perform(get("/api/transactions/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details[0]").value("No transaction found with id 999999"));
    }

    @Test
    void anInvalidDecisionFilterGivesA400() throws Exception {
        mockMvc.perform(get("/api/transactions").param("decision", "MAYBE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theEvaluationReportIsAvailable() throws Exception {
        mockMvc.perform(get("/api/model/evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.combined.recall").isNumber())
                .andExpect(jsonPath("$.modelWeights").isMap())
                .andExpect(jsonPath("$.recallByPattern.ACCOUNT_TAKEOVER").isNumber());
    }
}
