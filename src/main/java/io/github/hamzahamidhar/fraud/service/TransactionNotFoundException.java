package io.github.hamzahamidhar.fraud.service;

/** Thrown when someone asks for a payment ID that doesn't exist. Becomes a 404 response. */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(long id) {
        super("No transaction found with id " + id);
    }
}
