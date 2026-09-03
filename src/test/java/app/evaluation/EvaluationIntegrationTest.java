package app.evaluation;

import app.evaluation.domain.LlmConfigurationException;
import app.evaluation.domain.RateLimitedException;
import app.evaluation.domain.UpstreamUnavailableException;
import app.evaluation.llm.FakeLlmClient;
import app.evaluation.persistence.Evaluation;
import app.evaluation.persistence.EvaluationDocument;
import app.evaluation.persistence.FindingDocument;
import app.evaluation.web.EvaluationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
@Import(EvaluationTestConfig.class)
class EvaluationIntegrationTest extends AbstractEvaluationIntegrationTest {

    @Autowired
    private FakeLlmClient fakeLlmClient;

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

    @BeforeEach
    void resetFakeLlmClient() {
        fakeLlmClient.reset();
    }

    private String requestBody() throws Exception {
        return objectMapper.writeValueAsString(new EvaluationRequest(SUBMISSION_TEXT));
    }

    /**
     * The shape every rejection-rule test shares: the fake port returns the same rejected
     * payload on both the initial call and the re-ask, the endpoint answers 503 with the
     * {@code invalid_model_output} cause, nothing is persisted, and the model was called
     * exactly twice — the initial attempt plus exactly one re-ask, not a loop.
     */
    private void assertRejectedAfterOneReAsk(String rejectedPayload) throws Exception {
        fakeLlmClient.enqueue(rejectedPayload);
        fakeLlmClient.enqueue(rejectedPayload);
        long countBefore = evaluationRepository.count();

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("invalid_model_output"));

        assertThat(evaluationRepository.count()).isEqualTo(countBefore);
        assertThat(fakeLlmClient.callCount()).isEqualTo(2);
    }

    @Test
    void postingSubmissionReturnsStructuredEvaluationInDanish() throws Exception {
        fakeLlmClient.enqueue(VALID_MODEL_PAYLOAD);

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
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
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
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
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
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
    void malformedJsonFromTheModelIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        assertRejectedAfterOneReAsk("```json\n{ this is not valid JSON");
    }

    @Test
    void payloadReferencingACriterionOutsideTheRubricIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        assertRejectedAfterOneReAsk(PAYLOAD_WITH_UNKNOWN_CRITERION);
    }

    @Test
    void payloadMissingACriterionIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        JsonNode root = objectMapper.readTree(VALID_MODEL_PAYLOAD);
        ArrayNode findings = (ArrayNode) root.get("findings");
        findings.remove(findings.size() - 1);

        assertRejectedAfterOneReAsk(objectMapper.writeValueAsString(root));
    }

    @Test
    void payloadWithALevelNameOutsideTheRubricIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        JsonNode root = objectMapper.readTree(VALID_MODEL_PAYLOAD);
        ((ObjectNode) root.get("findings").get(0)).put("level", "Fremragende");

        assertRejectedAfterOneReAsk(objectMapper.writeValueAsString(root));
    }

    @Test
    void payloadWithFewerThanFourDialogueQuestionsIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        JsonNode root = objectMapper.readTree(VALID_MODEL_PAYLOAD);
        ArrayNode questions = (ArrayNode) root.get("dialogueQuestions");
        questions.remove(0);

        assertRejectedAfterOneReAsk(objectMapper.writeValueAsString(root));
    }

    @Test
    void payloadWithMoreThanSixDialogueQuestionsIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        JsonNode root = objectMapper.readTree(VALID_MODEL_PAYLOAD);
        ArrayNode questions = (ArrayNode) root.get("dialogueQuestions");
        questions.add("Hvad tager du med dig videre fra praktikken?");
        questions.add("Hvordan vil du gribe en lignende opgave an fremover?");
        questions.add("Hvilken kollega lærte du mest af, og hvorfor?");

        assertRejectedAfterOneReAsk(objectMapper.writeValueAsString(root));
    }

    @Test
    void payloadWithAFabricatedQuoteIsRejectedAfterOneReAskAndNothingIsPersisted() throws Exception {
        String payloadWithFabricatedQuote = VALID_MODEL_PAYLOAD.replace(
                "Jeg spurgte altid om hjælp når jeg var i tvivl, og passede på mine kolleger.",
                "Jeg reddede virksomhedens omsætning alene med en genial idé, som ingen andre så.");

        assertRejectedAfterOneReAsk(payloadWithFabricatedQuote);
    }

    @Test
    void evidenceQuoteThatOnlyDiffersByWhitespaceAcrossALineBreakIsAccepted() throws Exception {
        String submissionWithMidSentenceLineBreak = """
                Praktikvirksomheden er beskrevet grundigt i denne del af rapporten.
                Jeg brugte C# og React i mit daglige arbejde med kundeprojekter.
                Jeg deltog i code review hver uge sammen med teamet.
                Jeg viste initiativ ved at foreslå en ny arbejdsgang for teamet, som gav

                bedre overblik over opgaverne for alle involverede.
                Jeg reflekterede løbende over min egen udvikling gennem forløbet.
                Jeg delte min viden med de andre praktikanter og passede på dem.
                """;

        String payload = """
                {
                  "overallAssessment": "Rapporten giver et solidt førsteindtryk.",
                  "suggestedGrade": "10",
                  "findings": [
                    { "criterion": "formkrav", "level": "Tilfredsstillende",
                      "strengths": ["Klar struktur."], "weaknesses": ["Mindre mangler."],
                      "improvements": ["Uddyb et afsnit."],
                      "evidence": ["Praktikvirksomheden er beskrevet grundigt i denne del af rapporten."] },
                    { "criterion": "viden", "level": "Udmærket",
                      "strengths": ["God indsigt."], "weaknesses": ["Kunne uddybes."],
                      "improvements": ["Beskriv en kunde."],
                      "evidence": ["Jeg brugte C# og React i mit daglige arbejde med kundeprojekter."] },
                    { "criterion": "faerdigheder", "level": "Tilfredsstillende",
                      "strengths": ["Konkrete værktøjer nævnt."], "weaknesses": ["Afvejning mangler."],
                      "improvements": ["Beskriv valget af værktøjer."],
                      "evidence": ["Jeg viste initiativ ved at foreslå en ny arbejdsgang for teamet, som gav bedre overblik over opgaverne for alle involverede."] },
                    { "criterion": "kompetencer", "level": "Acceptabelt",
                      "strengths": ["Samarbejde nævnt."], "weaknesses": ["Eget bidrag utydeligt."],
                      "improvements": ["Beskriv en konkret situation."],
                      "evidence": ["Jeg deltog i code review hver uge sammen med teamet."] },
                    { "criterion": "refleksion", "level": "Tilfredsstillende",
                      "strengths": ["Reflekterer over egen udvikling."], "weaknesses": ["Teorikobling mangler."],
                      "improvements": ["Inddrag en model fra uddannelsen."],
                      "evidence": ["Jeg reflekterede løbende over min egen udvikling gennem forløbet."] },
                    { "criterion": "dare-share-care", "level": "Udmærket",
                      "strengths": ["Deler viden med andre."], "weaknesses": ["Kan uddybes."],
                      "improvements": ["Tilføj en konkret episode."],
                      "evidence": ["Jeg delte min viden med de andre praktikanter og passede på dem."] }
                  ],
                  "dialogueQuestions": [
                    "Hvordan valgte du din løsning?",
                    "Hvad lærte du af samarbejdet?",
                    "Hvilken teori kunne du have inddraget?",
                    "Hvad vil du gøre anderledes næste gang?"
                  ]
                }
                """;

        fakeLlmClient.enqueue(payload);

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EvaluationRequest(submissionWithMidSentenceLineBreak))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", hasSize(6)));

        assertThat(fakeLlmClient.callCount()).isEqualTo(1);
    }

    @Test
    void rejectedPayloadIsReAskedOnceAndSucceedsOnRetry() throws Exception {
        fakeLlmClient.enqueue("this is not json at all");
        fakeLlmClient.enqueue(VALID_MODEL_PAYLOAD);

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", hasSize(6)));

        assertThat(fakeLlmClient.callCount()).isEqualTo(2);
    }

    /**
     * The shape every provider-failure test shares: the fake port raises the failure the real
     * {@link app.evaluation.llm.OpenAiClient} would only raise after its own retry or fail-fast
     * handling is exhausted, so from the endpoint's perspective this is a single call — the
     * evaluation service never retries a provider failure, only a rejected payload (ticket 04).
     */
    private void assertFailsWithoutPersisting(RuntimeException failure, int expectedStatus, String expectedCode)
            throws Exception {
        fakeLlmClient.enqueueFailure(failure);
        long countBefore = evaluationRepository.count();

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));

        assertThat(evaluationRepository.count()).isEqualTo(countBefore);
        assertThat(fakeLlmClient.callCount()).isEqualTo(1);
    }

    @Test
    void rateLimitedProviderFailureReturnsServiceUnavailableWithRateLimitedCauseAndPersistsNothing()
            throws Exception {
        assertFailsWithoutPersisting(
                new RateLimitedException("OpenAI rate-limited the request", new RuntimeException("429")),
                503, "rate_limited");
    }

    @Test
    void upstreamUnavailableProviderFailureReturnsServiceUnavailableWithUpstreamUnavailableCauseAndPersistsNothing()
            throws Exception {
        assertFailsWithoutPersisting(
                new UpstreamUnavailableException("OpenAI returned a server error", new RuntimeException("503")),
                503, "upstream_unavailable");
    }

    @Test
    void configurationFaultReturnsServerErrorDistinctFromServiceUnavailableFamilyAndPersistsNothing()
            throws Exception {
        assertFailsWithoutPersisting(
                new LlmConfigurationException("OPENAI_API_KEY is not set"),
                500, "configuration_error");
    }

    @Test
    void findingsAreReturnedInRubricOrderRegardlessOfThePayloadsOrder() throws Exception {
        JsonNode root = objectMapper.readTree(VALID_MODEL_PAYLOAD);
        ArrayNode findings = (ArrayNode) root.get("findings");
        ArrayNode reversedFindings = objectMapper.createArrayNode();
        for (int i = findings.size() - 1; i >= 0; i--) {
            reversedFindings.add(findings.get(i));
        }
        ((ObjectNode) root).set("findings", reversedFindings);

        fakeLlmClient.enqueue(objectMapper.writeValueAsString(root));

        mockMvc.perform(post("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[0].criterion").value("formkrav"))
                .andExpect(jsonPath("$.findings[1].criterion").value("viden"))
                .andExpect(jsonPath("$.findings[2].criterion").value("faerdigheder"))
                .andExpect(jsonPath("$.findings[3].criterion").value("kompetencer"))
                .andExpect(jsonPath("$.findings[4].criterion").value("refleksion"))
                .andExpect(jsonPath("$.findings[5].criterion").value("dare-share-care"));
    }
}
