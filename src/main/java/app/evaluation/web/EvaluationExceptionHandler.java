package app.evaluation.web;

import app.evaluation.domain.EvaluationNotFoundException;
import app.evaluation.domain.InvalidModelOutputException;
import app.evaluation.domain.LlmConfigurationException;
import app.evaluation.domain.RateLimitedException;
import app.evaluation.domain.UpstreamUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the failure taxonomy to HTTP responses. {@code rate_limited}, {@code
 * upstream_unavailable} and {@code invalid_model_output} share {@code 503} — the request may
 * succeed if retried later. {@code configuration_error} is {@code 500}, kept out of that family
 * because it is our bug, not the provider's outage, and retrying it changes nothing. {@code
 * evaluation_not_found} is a plain {@code 404} — a read for an id that never existed or already
 * doesn't, not a provider failure at all.
 */
@RestControllerAdvice
public class EvaluationExceptionHandler {

    @ExceptionHandler(EvaluationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEvaluationNotFound(EvaluationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("evaluation_not_found", e.getMessage()));
    }

    @ExceptionHandler(InvalidModelOutputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModelOutput(InvalidModelOutputException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("invalid_model_output", e.getMessage()));
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ErrorResponse> handleRateLimited(RateLimitedException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("rate_limited", e.getMessage()));
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamUnavailable(UpstreamUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("upstream_unavailable", e.getMessage()));
    }

    @ExceptionHandler(LlmConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationFault(LlmConfigurationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("configuration_error", e.getMessage()));
    }
}
