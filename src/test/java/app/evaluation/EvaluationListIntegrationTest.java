package app.evaluation;

import app.evaluation.persistence.Evaluation;
import app.evaluation.persistence.EvaluationDocument;
import app.evaluation.persistence.EvaluationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real HTTP endpoint against a real Postgres (Testcontainers). This endpoint never
 * calls the language model, so fixtures are inserted directly through the repository rather
 * than via POST.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class EvaluationListIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EvaluationRepository evaluationRepository;

    private Evaluation persistEvaluation(String provider, String model, String suggestedGrade, Instant createdAt) {
        Evaluation evaluation = new Evaluation(
                UUID.randomUUID(), 1, provider, model, suggestedGrade, createdAt,
                new EvaluationDocument("assessment", List.of(), List.of("question?")));
        return evaluationRepository.save(evaluation);
    }

    @Test
    void listingReturnsSummariesNewestFirst() throws Exception {
        evaluationRepository.deleteAll();
        Evaluation older = persistEvaluation("openai", "gpt-4o-mini", "7", Instant.parse("2026-01-01T00:00:00Z"));
        Evaluation newer = persistEvaluation("openai", "gpt-4o-mini", "10", Instant.parse("2026-02-01T00:00:00Z"));

        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].evaluationId").value(newer.getId().toString()))
                .andExpect(jsonPath("$[0].rubricVersion").value(1))
                .andExpect(jsonPath("$[0].provider").value("openai"))
                .andExpect(jsonPath("$[0].model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$[0].suggestedGrade.value").value("10"))
                .andExpect(jsonPath("$[0].suggestedGrade.advisory").value(true))
                .andExpect(jsonPath("$[0].overallAssessment").doesNotExist())
                .andExpect(jsonPath("$[0].findings").doesNotExist())
                .andExpect(jsonPath("$[0].dialogueQuestions").doesNotExist())
                .andExpect(jsonPath("$[1].evaluationId").value(older.getId().toString()))
                .andExpect(jsonPath("$[1].suggestedGrade.value").value("7"));
    }

    @Test
    void listingAgainstAnEmptyStoreReturnsAnEmptyArray() throws Exception {
        evaluationRepository.deleteAll();

        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
