package io.github.hamzahamidhar.fraud.service;

import io.github.hamzahamidhar.fraud.detection.CustomerProfile;
import io.github.hamzahamidhar.fraud.detection.FraudAssessment;
import io.github.hamzahamidhar.fraud.detection.FraudDetector;
import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.domain.CardTransaction;
import io.github.hamzahamidhar.fraud.domain.Decision;
import io.github.hamzahamidhar.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * The business logic layer: what actually happens when a payment comes in.
 *
 * The controller deals with HTTP, the repository deals with the database, and this
 * class sits in between and makes the decisions. Keeping those three jobs apart
 * means each one is small, and we can change one (say, swap H2 for PostgreSQL)
 * without touching the others.
 *
 * Spring creates one instance of this class and "injects" what it needs through
 * the constructor. We never write "new TransactionService(...)" ourselves.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final FraudDetector detector;
    private final Clock clock;

    public TransactionService(TransactionRepository repository, FraudDetector detector, Clock clock) {
        this.repository = repository;
        this.detector = detector;
        this.clock = clock;
    }

    /**
     * Scores a new payment and saves it along with the verdict.
     *
     * The steps are: load the customer's recent history, summarise it into a profile,
     * ask the detector, and store the result. @Transactional means that if anything
     * fails halfway, nothing half-finished is left in the database.
     */
    @Transactional
    public CardTransaction process(TransactionDetails details) {
        Instant asOf = details.occurredAt();
        List<TransactionDetails> history = repository
                .findByCustomerIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        details.customerId(), asOf.minus(CustomerProfile.LOOKBACK), asOf)
                .stream()
                .map(CardTransaction::toDetails)
                .toList();

        CustomerProfile profile = CustomerProfile.fromHistory(history, asOf);
        FraudAssessment assessment = detector.assess(details, profile);
        return repository.save(new CardTransaction(details, assessment));
    }

    @Transactional(readOnly = true)
    public CardTransaction find(long id) {
        return repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    /** The 50 most recent payments, optionally only those with one decision. */
    @Transactional(readOnly = true)
    public List<CardTransaction> recent(Decision decision) {
        if (decision == null) {
            return repository.findTop50ByOrderByOccurredAtDesc();
        }
        return repository.findTop50ByDecisionInOrderByOccurredAtDesc(EnumSet.of(decision));
    }

    /** The 50 most recent payments that need attention (REVIEW or BLOCK). */
    @Transactional(readOnly = true)
    public List<CardTransaction> flagged() {
        return repository.findTop50ByDecisionInOrderByOccurredAtDesc(EnumSet.of(Decision.REVIEW, Decision.BLOCK));
    }

    @Transactional(readOnly = true)
    public List<CardTransaction> forCustomer(String customerId) {
        return repository.findTop50ByCustomerIdOrderByOccurredAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        return new Stats(
                repository.count(),
                repository.countByDecision(Decision.APPROVE),
                repository.countByDecision(Decision.REVIEW),
                repository.countByDecision(Decision.BLOCK));
    }

    /** The current time. Going through a Clock (rather than Instant.now()) lets tests control time. */
    public Instant now() {
        return clock.instant();
    }

    /** Simple counts for the dashboard. */
    public record Stats(long total, long approved, long review, long blocked) {
    }
}
