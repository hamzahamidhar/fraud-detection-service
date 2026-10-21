package io.github.hamzahamidhar.fraud.repository;

import io.github.hamzahamidhar.fraud.domain.CardTransaction;
import io.github.hamzahamidhar.fraud.domain.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Everything we need from the database, without writing a line of SQL.
 *
 * Spring Data reads each method name and writes the query for us. For example,
 * findTop50ByCustomerIdOrderByOccurredAtDesc becomes roughly:
 *
 *   SELECT * FROM card_transactions WHERE customer_id = ?
 *   ORDER BY occurred_at DESC LIMIT 50
 *
 * Basic operations like save() and findById() come free from JpaRepository.
 */
public interface TransactionRepository extends JpaRepository<CardTransaction, Long> {

    /** A customer's payments in a time window; used to build their profile. */
    List<CardTransaction> findByCustomerIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            String customerId, Instant from, Instant to);

    List<CardTransaction> findTop50ByCustomerIdOrderByOccurredAtDesc(String customerId);

    List<CardTransaction> findTop50ByOrderByOccurredAtDesc();

    List<CardTransaction> findTop50ByDecisionInOrderByOccurredAtDesc(Collection<Decision> decisions);

    long countByDecision(Decision decision);
}
