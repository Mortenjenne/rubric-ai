package app.assignment.web;

import java.util.List;
import java.util.Map;

/**
 * One Criterion in a {@link ReplaceDraftRequest}. {@code key} is absent (or blank) for a new
 * Criterion; present to keep an existing Criterion's identity across a rename or a reorder. No
 * field here is validated — a half-written Rubric must stay saveable.
 */
public record DraftCriterionRequest(String key, String name, int weight, String description,
                                     List<String> sourceReferences, Map<String, String> levels) {
}
