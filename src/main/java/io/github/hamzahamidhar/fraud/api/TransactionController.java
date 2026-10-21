package io.github.hamzahamidhar.fraud.api;

import io.github.hamzahamidhar.fraud.detection.TransactionDetails;
import io.github.hamzahamidhar.fraud.domain.CardTransaction;
import io.github.hamzahamidhar.fraud.domain.Decision;
import io.github.hamzahamidhar.fraud.evaluation.EvaluationReport;
import io.github.hamzahamidhar.fraud.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * The REST API: the "front door" other programs use to talk to the service.
 *
 * Each method maps one URL to one action. The controller's only job is to
 * translate between HTTP and Java: read the request, hand the real work to
 * {@link TransactionService}, and turn the result into a response. If you find
 * business logic creeping into a controller, it usually belongs in the service.
 *
 * Endpoints:
 *   POST /api/transactions                 score and save a new payment
 *   GET  /api/transactions                 recent payments (optional ?decision=BLOCK)
 *   GET  /api/transactions/flagged         recent payments that need attention
 *   GET  /api/transactions/{id}            one payment
 *   GET  /api/customers/{id}/transactions  one customer's recent payments
 *   GET  /api/stats                        counts for the dashboard
 *   GET  /api/model/evaluation             how well the detector performed in testing
 */
@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService service;
    private final EvaluationReport evaluationReport;

    public TransactionController(TransactionService service, EvaluationReport evaluationReport) {
        this.service = service;
        this.evaluationReport = evaluationReport;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED) // 201 means "we made something new for you"
    public TransactionResponse submit(@Valid @RequestBody TransactionRequest request) {
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : service.now();
        TransactionDetails details = new TransactionDetails(
                request.customerId(),
                request.amount(),
                request.merchantCategory(),
                request.country(),
                occurredAt);
        return TransactionResponse.from(service.process(details));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> recent(@RequestParam(name = "decision", required = false) Decision decision) {
        return toResponses(service.recent(decision));
    }

    @GetMapping("/transactions/flagged")
    public List<TransactionResponse> flagged() {
        return toResponses(service.flagged());
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse one(@PathVariable("id") long id) {
        return TransactionResponse.from(service.find(id));
    }

    @GetMapping("/customers/{customerId}/transactions")
    public List<TransactionResponse> forCustomer(@PathVariable("customerId") String customerId) {
        return toResponses(service.forCustomer(customerId));
    }

    @GetMapping("/stats")
    public TransactionService.Stats stats() {
        return service.stats();
    }

    @GetMapping("/model/evaluation")
    public EvaluationReport evaluation() {
        return evaluationReport;
    }

    private static List<TransactionResponse> toResponses(List<CardTransaction> transactions) {
        return transactions.stream().map(TransactionResponse::from).toList();
    }
}
