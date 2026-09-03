package app.assignment.json;

import java.util.List;

/**
 * Shape of a bundled Rubric JSON resource under {@code classpath:rubric/}. Unread by anything
 * until a Template consumes it (ticket 04).
 */
public record RubricResource(
        int version,
        String assignment,
        List<CriterionResource> criteria) {
}
