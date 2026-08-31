package app.evaluation.json;

import jakarta.validation.constraints.NotBlank;

public record EvaluationRequest(@NotBlank String submissionText) {
}
