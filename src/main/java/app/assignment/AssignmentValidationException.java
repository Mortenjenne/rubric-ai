package app.assignment;

import java.util.List;

/**
 * Publishing's structural checks failed. Carries every failure at once, not just the first, so an
 * Educator sees the whole list of what to fix in one response.
 */
public class AssignmentValidationException extends RuntimeException {

    private final List<String> errors;

    public AssignmentValidationException(List<String> errors) {
        super("Draft failed publish validation: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
