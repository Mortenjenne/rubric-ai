package app.evaluation.persistence;

import java.util.List;

/**
 * One Finding as stored inside an Evaluation's JSON document: the model's verdict on a
 * Criterion, alongside that Criterion's name and Weight as recorded in the Rubric version
 * that judged it — a snapshot, so a later Rubric edit cannot change what this Evaluation said.
 */
public record FindingDocument(
        String criterion,
        String criterionName,
        int weight,
        String level,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvements,
        List<String> evidence) {
}
