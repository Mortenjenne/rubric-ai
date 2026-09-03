package app.evaluation;

import app.evaluation.persistence.Evaluation;
import app.evaluation.persistence.EvaluationDocument;
import app.evaluation.persistence.FindingDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real HTTP endpoint against a real Postgres (Testcontainers). This endpoint never
 * calls the language model, so the fixture is inserted directly through the repository rather
 * than via POST.
 */
class EvaluationByIdIntegrationTest extends AbstractEvaluationIntegrationTest {

    @Test
    void fetchingAPersistedEvaluationByIdReturnsTheFullEvaluation() throws Exception {
        FindingDocument finding = new FindingDocument(
                "formkrav", "Formkrav & begrænsninger", 10, "Tilfredsstillende",
                List.of("Rapporten er velstruktureret."),
                List.of("Et enkelt afsnit mangler et konkret eksempel."),
                List.of("Tilføj en kort beskrivelse af evalueringsskemaet."),
                List.of("Jeg brugte C# og React."));
        Evaluation evaluation = new Evaluation(
                UUID.randomUUID(), 1, "openai", "gpt-4o-mini", "10", Instant.parse("2026-02-01T00:00:00Z"),
                new EvaluationDocument(
                        "Rapporten giver et solidt første indtryk.",
                        List.of(finding),
                        List.of("Hvordan valgte du løsningen?", "Hvad lærte du?")));
        evaluationRepository.save(evaluation);

        mockMvc.perform(get("/api/evaluations/{id}", evaluation.getId())
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(evaluation.getId().toString()))
                .andExpect(jsonPath("$.rubricVersion").value(1))
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.overallAssessment").value("Rapporten giver et solidt første indtryk."))
                .andExpect(jsonPath("$.suggestedGrade.value").value("10"))
                .andExpect(jsonPath("$.suggestedGrade.advisory").value(true))
                .andExpect(jsonPath("$.findings", hasSize(1)))
                .andExpect(jsonPath("$.findings[0].criterion").value("formkrav"))
                .andExpect(jsonPath("$.findings[0].criterionName").value("Formkrav & begrænsninger"))
                .andExpect(jsonPath("$.findings[0].weight").value(10))
                .andExpect(jsonPath("$.findings[0].level").value("Tilfredsstillende"))
                .andExpect(jsonPath("$.dialogueQuestions", hasSize(2)));
    }

    @Test
    void fetchingAnUnknownIdReturns404WithEvaluationNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/evaluations/{id}", unknownId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("evaluation_not_found"));
    }

    /**
     * Documented boundary case, not a newly handled one: a non-UUID path segment falls through
     * to Spring's default error body, the same known gap already documented for a blank
     * {@code submissionText} on {@code POST}. See the spec's testing decisions.
     */
    @Test
    void fetchingAMalformedIdReturnsTheFrameworksDefaultBadRequestBody() throws Exception {
        mockMvc.perform(get("/api/evaluations/{id}", "not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
