package app.evaluation.web;

import java.time.Instant;

public record EvaluationSummaryResponse(
        String evaluationId,
        int rubricVersion,
        String provider,
        String model,
        Instant createdAt,
        SuggestedGradeResponse suggestedGrade) {
}
