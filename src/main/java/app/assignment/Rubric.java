package app.assignment;

import java.util.List;

/**
 * The Criteria-and-Levels value inside a Draft or an AssignmentVersion: what a teacher means by
 * the word "rubric". Not itself persisted — it is a read-only view over whichever list of
 * Criterion rows its owner (a Draft or an AssignmentVersion) actually holds.
 */
public final class Rubric {

    private final List<Criterion> criteria;

    public Rubric(List<Criterion> criteria) {
        this.criteria = List.copyOf(criteria);
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }
}
