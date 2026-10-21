package io.github.hamzahamidhar.fraud.api;

import java.util.List;

/**
 * The shape of every error we return, so clients always know what to expect:
 *
 * <pre>
 * { "status": 400, "error": "Invalid request", "details": ["amount must be more than zero"] }
 * </pre>
 */
public record ApiError(int status, String error, List<String> details) {
}
