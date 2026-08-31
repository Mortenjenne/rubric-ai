package app.evaluation.llm;

import app.evaluation.domain.Grade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The shape the model's raw JSON is deserialised into before anything is trusted. Bean
 * Validation runs on this record; a failure here means the payload never reaches persistence
 * or the Educator. The advisory flag is not part of this shape — the model is never asked for
 * it, and the service sets it unconditionally when building the response.
 */
public record LlmEvaluationPayload(
        @NotBlank String overallAssessment,
        @NotNull Grade suggestedGrade,
        @NotEmpty @Valid List<LlmFindingPayload> findings,
        @NotNull @Size(min = 4, max = 6) List<@NotBlank String> dialogueQuestions) {
}
