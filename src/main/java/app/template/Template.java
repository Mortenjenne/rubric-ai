package app.template;

import java.util.List;

/**
 * A classpath-bundled starting point for an Assignment: a Rubric and an Assessment stance, and
 * no Source material — course documents belong to a course, not to a starting shape. Never
 * persisted and never owned by an Educator. Copying one into an Assignment builds brand new
 * Criterion rows, so nothing in the copy is shared with the Template or with any other
 * Assignment created from it.
 */
public record Template(String id, String title, String description, String assessmentStance,
                        List<TemplateCriterion> criteria) {
}
