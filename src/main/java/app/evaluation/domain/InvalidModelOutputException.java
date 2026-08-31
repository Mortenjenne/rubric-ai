package app.evaluation.domain;

/**
 * The model's raw response could not be trusted: malformed JSON, a Bean Validation failure, a
 * Finding that references a Criterion outside the active Rubric or omits one that's in it, or
 * an evidence quote that is not a verbatim excerpt of the Submission. Raised instead of
 * persisting or returning anything — an Educator never sees a half-built Evaluation. The
 * service re-asks the model once on this exception before giving up.
 */
public class InvalidModelOutputException extends RuntimeException {

    public InvalidModelOutputException(String message) {
        super(message);
    }

    public InvalidModelOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
