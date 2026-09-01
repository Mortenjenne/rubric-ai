package app.evaluation.domain;

/**
 * No persisted Evaluation matches the requested id. Distinct from the provider-failure
 * taxonomy — this is a client-facing 404, not a 5xx: the id is simply wrong, not something a
 * retry could fix.
 */
public class EvaluationNotFoundException extends RuntimeException {

    public EvaluationNotFoundException(String message) {
        super(message);
    }
}
