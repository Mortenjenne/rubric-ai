package app.assignment;

import java.util.List;
import java.util.Map;

/**
 * One Criterion in a whole-Draft replacement (see {@link Assignment#replaceDraft}). {@code key}
 * is {@code null} for a new Criterion, which is assigned the aggregate's next sequential key; a
 * non-null {@code key} keeps an existing Criterion's identity across a rename or a reorder.
 */
public record DraftCriterionInput(String key, String name, int weight, String description,
                                   List<String> sourceReferences, Map<String, String> levels) {
}
