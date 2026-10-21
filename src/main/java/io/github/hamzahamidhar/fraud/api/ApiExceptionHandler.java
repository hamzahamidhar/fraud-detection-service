package io.github.hamzahamidhar.fraud.api;

import io.github.hamzahamidhar.fraud.service.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Turns exceptions into friendly JSON errors.
 *
 * Without this, a bad request would get Spring's default error page, which is
 * fine for developers but unhelpful for anyone calling the API. Putting all the
 * error handling in one class also keeps the controller clean.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** A field failed one of the checks in TransactionRequest. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        List<String> problems = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Invalid request", problems);
    }

    /** The body wasn't valid JSON, or a value had the wrong type (e.g. an unknown merchant category). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request",
                List.of("The request body could not be read. Check the JSON and that merchantCategory "
                        + "is one of the supported values."));
    }

    /** A URL parameter had the wrong type, e.g. ?decision=MAYBE. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request",
                List.of("'" + exception.getValue() + "' is not a valid value for " + exception.getName()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> notFound(TransactionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Not found", List.of(exception.getMessage()));
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String title, List<String> details) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), title, details));
    }
}
