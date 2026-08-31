package app.evaluation.web;

import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        String evaluationId,
        int rubricVersion,
        String provider,
        String model,
        Instant createdAt,
        String overallAssessment,
        SuggestedGradeResponse suggestedGrade,
        List<FindingResponse> findings,
        List<String> dialogueQuestions) {
}
