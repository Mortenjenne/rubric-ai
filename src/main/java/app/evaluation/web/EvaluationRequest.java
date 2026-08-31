package app.evaluation.web;

import jakarta.validation.constraints.NotBlank;

public record EvaluationRequest(@NotBlank String submissionText) {
}
