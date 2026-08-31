package app.rubric.json;

import java.util.List;

/**
 * Shape of the bundled Rubric JSON resource under {@code classpath:rubric/}.
 */
public record RubricResource(
        int version,
        String assignment,
        String language,
        List<String> levels,
        String note,
        List<CriterionResource> criteria) {
}
