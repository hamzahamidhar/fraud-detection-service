package io.github.hamzahamidhar.fraud.domain;

import io.github.hamzahamidhar.fraud.detection.FraudAssessment;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A card payment as it is stored in the database, together with our verdict on it.
 *
 * The annotations tell JPA (Hibernate, under the hood) how to map this class onto
 * database tables, so we never have to write SQL to save or load one. Each field
 * becomes a column, and the list of reasons gets its own small table because one
 * payment can have several reasons.
 *
 * The index on (customer_id, occurred_at) matters: every new payment triggers a
 * lookup of "this customer's recent payments", and the index makes that fast even
 * when the table holds millions of rows.
 */
@Entity
@Table(name = "card_transactions",
        indexes = @Index(name = "idx_customer_time", columnList = "customer_id, occurred_at"))
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING) // Store "GIFT_CARDS", not a number that breaks if the enum is reordered.
    @Column(name = "merchant_category", nullable = false, length = 30)
    private MerchantCategory merchantCategory;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Decision decision;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "transaction_reasons", joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "reason", length = 300)
    private List<String> reasons = new ArrayList<>();

    /** JPA needs an empty constructor to create objects when loading from the database. */
    protected CardTransaction() {
    }

    public CardTransaction(TransactionDetails details, FraudAssessment assessment) {
        this.customerId = details.customerId();
        this.amount = details.amount();
        this.merchantCategory = details.merchantCategory();
        this.country = details.country();
        this.occurredAt = details.occurredAt();
        this.riskScore = assessment.riskScore();
        this.decision = assessment.decision();
        this.reasons = new ArrayList<>(assessment.reasons());
    }

    /** Converts back into the plain form the detector understands. */
    public TransactionDetails toDetails() {
        return new TransactionDetails(customerId, amount, merchantCategory, country, occurredAt);
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public MerchantCategory getMerchantCategory() {
        return merchantCategory;
    }

    public String getCountry() {
        return country;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public Decision getDecision() {
        return decision;
    }

    public List<String> getReasons() {
        return List.copyOf(reasons);
    }
}
