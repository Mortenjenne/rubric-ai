package app.evaluation.web;

import app.evaluation.domain.InvalidModelOutputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the failure taxonomy to HTTP responses. Only {@code invalid_model_output} exists yet;
 * the retry-driven {@code rate_limited} and {@code upstream_unavailable} causes land with the
 * provider adapter and its retry policy in later tickets.
 */
@RestControllerAdvice
public class EvaluationExceptionHandler {

    @ExceptionHandler(InvalidModelOutputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModelOutput(InvalidModelOutputException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("invalid_model_output", e.getMessage()));
    }
}
