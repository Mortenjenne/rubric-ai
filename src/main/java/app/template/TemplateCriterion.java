package app.template;

import java.util.List;
import java.util.Map;

/**
 * One row of a Template's Rubric, in exactly the shape
 * {@link app.assignment.Assignment#addDraftCriterion} expects when copying it into a new Draft.
 */
public record TemplateCriterion(String key, String name, int weight, String description,
                                 List<String> sourceReferences, Map<String, String> levels) {
}
