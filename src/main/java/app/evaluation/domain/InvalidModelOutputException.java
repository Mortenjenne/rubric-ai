package app.evaluation.domain;

/**
 * The model's raw response could not be trusted: malformed JSON, a Bean Validation failure,
 * or a Finding that does not reference a Criterion in the active Rubric. Raised instead of
 * persisting or returning anything — an Educator never sees a half-built Evaluation.
 */
public class InvalidModelOutputException extends RuntimeException {

    public InvalidModelOutputException(String message) {
        super(message);
    }

    public InvalidModelOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
