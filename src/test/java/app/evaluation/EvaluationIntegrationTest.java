package app.evaluation;

import app.evaluation.json.EvaluationRequest;
import app.evaluation.llm.FakeLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real HTTP endpoint against a real Postgres (Testcontainers) with everything
 * real except the language model, per the spec's testing decisions. Assertions land on the
 * response body and the database, never on prompt strings or internal calls.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(EvaluationTestConfig.class)
class EvaluationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeLlmClient fakeLlmClient;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String MARKER_SENTENCE =
            "Denne saetning bruges udelukkende til at bevise at teksten aldrig gemmes, kode QZX-77219.";

    private static final String SUBMISSION_TEXT = """
            Praktikvirksomheden er et mellemstort IT-konsulenthus med afdelinger i tre byer.
            Jeg arbejdede i udviklingsteamet med den daglige drift af kundeprojekter.
            Jeg brugte C# og React til at bygge en intern rapporteringsløsning til virksomheden.
            Jeg deltog i code review sammen med to seniorudviklere hver uge.
            %s
            Jeg foreslog en ny arbejdsgang for standupmøderne og turde tage initiativet selv.
            Jeg delte mine erfaringer med de andre praktikanter på et internt oplæg.
            Jeg spurgte altid om hjælp når jeg var i tvivl, og passede på mine kolleger.
            """.formatted(MARKER_SENTENCE);

    private static final String VALID_MODEL_PAYLOAD = """
            {
              "overallAssessment": "Rapporten giver et solidt første indtryk med konkrete eksempler fra praktikken.",
              "suggestedGrade": "10",
              "findings": [
                {
                  "criterion": "formkrav",
                  "level": "Tilfredsstillende",
                  "strengths": ["Rapporten er velstruktureret."],
                  "weaknesses": ["Et enkelt afsnit mangler et konkret eksempel."],
                  "improvements": ["Tilføj en kort beskrivelse af evalueringsskemaet."],
                  "evidence": ["Jeg brugte C# og React til at bygge en intern rapporteringsløsning til virksomheden."]
                },
                {
                  "criterion": "viden",
                  "level": "Udmærket",
                  "strengths": ["Virksomheden beskrives som helhed."],
                  "weaknesses": ["Kundernes branche kunne være uddybet."],
                  "improvements": ["Beskriv kort virksomhedens største kunder."],
                  "evidence": ["Praktikvirksomheden er et mellemstort IT-konsulenthus med afdelinger i tre byer."]
                },
                {
                  "criterion": "faerdigheder",
                  "level": "Tilfredsstillende",
                  "strengths": ["Konkrete tekniske værktøjer er navngivet."],
                  "weaknesses": ["Afvejning af løsningsmuligheder er ikke vist."],
                  "improvements": ["Beskriv hvorfor React blev valgt frem for alternativer."],
                  "evidence": ["Jeg brugte C# og React til at bygge en intern rapporteringsløsning til virksomheden."]
                },
                {
                  "criterion": "kompetencer",
                  "level": "Acceptabelt",
                  "strengths": ["Samarbejde med seniorudviklere er nævnt."],
                  "weaknesses": ["Eget bidrag til samarbejdet er utydeligt."],
                  "improvements": ["Beskriv en konkret situation fra code review."],
                  "evidence": ["Jeg deltog i code review sammen med to seniorudviklere hver uge."]
                },
                {
                  "criterion": "refleksion",
                  "level": "Tilfredsstillende",
                  "strengths": ["Der reflekteres over egen arbejdsgang."],
                  "weaknesses": ["Kobling til uddannelsens teori mangler."],
                  "improvements": ["Inddrag en navngiven model fra uddannelsen."],
                  "evidence": ["Jeg foreslog en ny arbejdsgang for standupmøderne og turde tage initiativet selv."]
                },
                {
                  "criterion": "dare-share-care",
                  "level": "Udmærket",
                  "strengths": ["Alle tre værdier er belagt med konkrete episoder."],
                  "weaknesses": ["Omsorg for andre kunne være uddybet yderligere."],
                  "improvements": ["Tilføj en episode om at hjælpe en anden praktikant."],
                  "evidence": ["Jeg spurgte altid om hjælp når jeg var i tvivl, og passede på mine kolleger."]
                }
              ],
              "dialogueQuestions": [
                "Hvordan valgte du mellem de tekniske løsninger, du overvejede?",
                "Hvad lærte du af samarbejdet med seniorudviklerne?",
                "Hvilken teori fra uddannelsen kunne du have inddraget?",
                "Hvad vil du gøre anderledes i en lignende situation fremover?"
              ]
            }
            """;

    private static final String PAYLOAD_WITH_UNKNOWN_CRITERION =
            VALID_MODEL_PAYLOAD.replace("\"criterion\": \"dare-share-care\"", "\"criterion\": \"unknown-criterion\"");

    private String requestBody() throws Exception {
        return objectMapper.writeValueAsString(new EvaluationRequest(SUBMISSION_TEXT));
    }

    @Test
    void postingSubmissionReturnsStructuredEvaluationInDanish() throws Exception {
        fakeLlmClient.enqueue(VALID_MODEL_PAYLOAD);

        mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").isNotEmpty())
                .andExpect(jsonPath("$.rubricVersion").value(1))
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.overallAssessment").isNotEmpty())
                .andExpect(jsonPath("$.suggestedGrade.value").value("10"))
                .andExpect(jsonPath("$.suggestedGrade.advisory").value(true))
                .andExpect(jsonPath("$.findings", hasSize(6)))
                .andExpect(jsonPath("$.findings[0].criterion").value("formkrav"))
                .andExpect(jsonPath("$.findings[0].criterionName").value("Formkrav & begrænsninger"))
                .andExpect(jsonPath("$.findings[0].weight").value(10))
                .andExpect(jsonPath("$.findings[1].criterion").value("viden"))
                .andExpect(jsonPath("$.findings[2].criterion").value("faerdigheder"))
                .andExpect(jsonPath("$.findings[3].criterion").value("kompetencer"))
                .andExpect(jsonPath("$.findings[4].criterion").value("refleksion"))
                .andExpect(jsonPath("$.findings[5].criterion").value("dare-share-care"))
                .andExpect(jsonPath("$.findings[*].level",
                        everyItem(oneOf("Mangelfuldt", "Acceptabelt", "Tilfredsstillende", "Udmærket"))))
                .andExpect(jsonPath("$.dialogueQuestions", hasSize(4)));
    }

    @Test
    void evaluationPersistsWithColumnsAndFindingsAsADocument() throws Exception {
        fakeLlmClient.enqueue(VALID_MODEL_PAYLOAD);

        String response = mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID evaluationId = UUID.fromString(objectMapper.readTree(response).get("evaluationId").asText());

        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(evaluation.getRubricVersion()).isEqualTo(1);
        assertThat(evaluation.getProvider()).isEqualTo("openai");
        assertThat(evaluation.getModel()).isNotBlank();
        assertThat(evaluation.getSuggestedGrade()).isEqualTo("10");
        assertThat(evaluation.getCreatedAt()).isNotNull();

        EvaluationDocument document = evaluation.getDocument();
        assertThat(document.overallAssessment()).isNotBlank();
        assertThat(document.findings()).hasSize(6);
        assertThat(document.findings()).extracting(FindingDocument::criterion)
                .containsExactly("formkrav", "viden", "faerdigheder", "kompetencer", "refleksion", "dare-share-care");
        assertThat(document.findings().get(2).criterionName()).isEqualTo("Færdigheder i praksis");
        assertThat(document.findings().get(2).weight()).isEqualTo(25);
        assertThat(document.dialogueQuestions()).hasSize(4);
    }

    @Test
    void submittedTextIsAbsentFromStorageAfterASuccessfulEvaluation() throws Exception {
        fakeLlmClient.enqueue(VALID_MODEL_PAYLOAD);

        String response = mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID evaluationId = UUID.fromString(objectMapper.readTree(response).get("evaluationId").asText());
        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElseThrow();

        String persisted = objectMapper.writeValueAsString(evaluation.getDocument());
        assertThat(persisted).doesNotContain(SUBMISSION_TEXT);
        assertThat(persisted).doesNotContain(MARKER_SENTENCE);
    }

    @Test
    void malformedJsonFromTheModelIsRejectedAndNothingIsPersisted() throws Exception {
        fakeLlmClient.enqueue("```json\n{ this is not valid JSON");
        long countBefore = evaluationRepository.count();

        mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("invalid_model_output"));

        assertThat(evaluationRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void payloadReferencingACriterionOutsideTheRubricIsRejectedAndNothingIsPersisted() throws Exception {
        fakeLlmClient.enqueue(PAYLOAD_WITH_UNKNOWN_CRITERION);
        long countBefore = evaluationRepository.count();

        mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("invalid_model_output"));

        assertThat(evaluationRepository.count()).isEqualTo(countBefore);
    }
}
