package app.evaluation.llm;

import app.assignment.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * One Finding as the model emits it — the Criterion's Weight and name are not asked of the
 * model; the service attaches them from the Rubric so a Finding cannot misreport what the
 * Rubric actually says about the Criterion it references.
 */
public record LlmFindingPayload(
        @NotBlank String criterion,
        @NotNull Level level,
        @NotEmpty List<@NotBlank String> strengths,
        @NotEmpty List<@NotBlank String> weaknesses,
        @NotEmpty List<@NotBlank String> improvements,
        @NotEmpty List<@NotBlank String> evidence) {
}
