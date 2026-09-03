package app.assignment.web;

import java.util.List;

/**
 * The whole Draft, replacing what was there before: a Criterion the list doesn't mention is
 * removed. Deliberately carries no validation of any kind — the title, the Assessment stance and
 * each Criterion's content are all unvalidated, so a half-written Rubric stays saveable.
 * Validation belongs to publishing, not to saving a Draft.
 */
public record ReplaceDraftRequest(String title, String assessmentStance, List<DraftCriterionRequest> criteria) {
}
