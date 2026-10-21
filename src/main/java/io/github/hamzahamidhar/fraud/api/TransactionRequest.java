package io.github.hamzahamidhar.fraud.api;

import io.github.hamzahamidhar.fraud.domain.MerchantCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The JSON a client sends to report a new payment, for example:
 *
 * <pre>
 * {
 *   "customerId": "C0001",
 *   "amount": 849.99,
 *   "merchantCategory": "ELECTRONICS",
 *   "country": "FR"
 * }
 * </pre>
 *
 * The annotations are validation rules. Spring checks them before our code runs,
 * and sends back a clear 400 error if any fail. Never trust input from outside:
 * checking it at the door means the rest of the code can assume it is sensible.
 *
 * occurredAt is optional. If it's missing we use the current time.
 */
public record TransactionRequest(

        @NotBlank(message = "is required")
        @Pattern(regexp = "[A-Za-z0-9_-]{1,20}", message = "must be 1-20 letters, digits, '-' or '_'")
        String customerId,

        @NotNull(message = "is required")
        @Positive(message = "must be more than zero")
        @DecimalMax(value = "1000000", message = "must be £1,000,000 or less")
        @Digits(integer = 10, fraction = 2, message = "can have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "is required")
        MerchantCategory merchantCategory,

        @NotBlank(message = "is required")
        @Pattern(regexp = "[A-Z]{2}", message = "must be a two-letter country code such as GB")
        String country,

        Instant occurredAt) {
}
