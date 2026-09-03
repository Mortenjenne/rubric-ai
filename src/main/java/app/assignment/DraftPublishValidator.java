package app.assignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one validation gate in the whole authoring flow, run only when publishing — never on
 * saving a Draft. Checks structure only: that a Rubric exists, that every Criterion describes
 * every Level, and that Criterion keys are unique. Never checks whether Weights sum to anything
 * or whether the Assessment stance is blank — both are guidance the project has consistently held
 * nothing multiplies by.
 */
final class DraftPublishValidator {

    private DraftPublishValidator() {
    }

    static List<String> errorsFor(Draft draft) {
        List<Criterion> criteria = draft.getCriteria();
        List<String> errors = new ArrayList<>();

        if (criteria.isEmpty()) {
            errors.add("The draft has no criteria.");
        }

        Map<String, Integer> occurrencesByKey = new LinkedHashMap<>();
        for (Criterion criterion : criteria) {
            occurrencesByKey.merge(criterion.getKey(), 1, Integer::sum);

            for (Level level : Level.values()) {
                String descriptor = criterion.getLevels().get(level.label());
                if (descriptor == null || descriptor.isBlank()) {
                    errors.add("Criterion '" + criterion.getKey() + "' is missing a descriptor for level '"
                            + level.label() + "'.");
                }
            }
        }

        occurrencesByKey.forEach((key, count) -> {
            if (count > 1) {
                errors.add("Criterion key '" + key + "' is used by " + count + " criteria.");
            }
        });

        return errors;
    }
}
