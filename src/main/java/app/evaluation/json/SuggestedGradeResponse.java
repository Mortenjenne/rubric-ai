package app.evaluation.json;

/**
 * {@code advisory} is always {@code true}: the service sets it unconditionally rather than
 * trusting the model to flag its own output, per ADR 0002.
 */
public record SuggestedGradeResponse(String value, boolean advisory) {

    public SuggestedGradeResponse(String value) {
        this(value, true);
    }
}
