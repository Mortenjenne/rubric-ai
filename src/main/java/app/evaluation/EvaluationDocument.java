package app.evaluation;

import java.util.List;

/**
 * The part of an Evaluation stored as a single JSON document rather than as columns: the
 * Submission text itself is never in here, per ADR 0003 — only the verbatim excerpts that
 * live inside each Finding's evidence.
 */
public record EvaluationDocument(
        String overallAssessment,
        List<FindingDocument> findings,
        List<String> dialogueQuestions) {
}
