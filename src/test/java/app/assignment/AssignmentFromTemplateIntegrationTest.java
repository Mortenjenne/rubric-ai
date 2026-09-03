package app.assignment;

import app.educator.Educator;
import app.evaluation.AbstractEvaluationIntegrationTest;
import app.template.Template;
import app.template.TemplateCatalog;
import app.template.TemplateCriterion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real {@code GET /api/templates} and {@code POST /api/assignments} endpoints against
 * a real Postgres (Testcontainers), reusing the evaluation feature's shared wiring. Uses its own
 * Educator rather than {@link #TEST_EDUCATOR_EMAIL} — that account's Assignment history is relied
 * on by {@link app.evaluation.EvaluationIntegrationTest}'s temporary seam (ticket 03), and the
 * Postgres container is shared across every integration test class.
 */
class AssignmentFromTemplateIntegrationTest extends AbstractEvaluationIntegrationTest {

    private static final String TEMPLATE_TEST_EDUCATOR_EMAIL = "template-test-educator@example.com";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateCatalog templateCatalog;

    @Autowired
    private AssignmentRepository assignmentRepository;

    private Educator educator;
    private String authorization;

    private void seedTemplateTestEducator() {
        educator = seedEducator(TEMPLATE_TEST_EDUCATOR_EMAIL, "Template Test Educator", "irrelevant-test-password");
        authorization = authorizationHeaderFor(educator);
    }

    @Test
    void listTemplatesReturnsBothShippedTemplatesWithTheirCriteriaNames() throws Exception {
        seedTemplateTestEducator();

        mockMvc.perform(get("/api/templates")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder("praktikrapport", "skriftlig-aflevering")))
                .andExpect(jsonPath("$[*].title", everyItem(not(emptyOrNullString()))))
                .andExpect(jsonPath("$[*].description", everyItem(not(emptyOrNullString()))))
                .andExpect(jsonPath("$[0].criteriaNames").isNotEmpty());
    }

    @Test
    void creatingAnAssignmentFromEachTemplateCopiesItsDraftFaithfully() throws Exception {
        seedTemplateTestEducator();

        for (Template template : templateCatalog.findAll()) {
            String response = mockMvc.perform(post("/api/assignments")
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createAssignmentBody(template.id())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value(template.title()))
                    .andExpect(jsonPath("$.draft.assessmentStance").value(template.assessmentStance()))
                    .andExpect(jsonPath("$.draft.criteria", hasSize(template.criteria().size())))
                    .andReturn().getResponse().getContentAsString();

            assertDraftCriteriaMatchTemplate(response, template.criteria());

            UUID assignmentId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
            Assignment persisted = assignmentRepository
                    .findByIdAndEducatorId(assignmentId, educator.getId())
                    .orElseThrow();
            assertThat(persisted.getVersions()).isEmpty();
        }
    }

    private void assertDraftCriteriaMatchTemplate(String response, List<TemplateCriterion> expected) throws Exception {
        JsonNode draftCriteria = objectMapper.readTree(response).get("draft").get("criteria");

        for (int i = 0; i < expected.size(); i++) {
            TemplateCriterion criterion = expected.get(i);
            JsonNode actual = draftCriteria.get(i);
            assertThat(actual.get("key").asText()).isEqualTo(criterion.key());
            assertThat(actual.get("name").asText()).isEqualTo(criterion.name());
            assertThat(actual.get("weight").asInt()).isEqualTo(criterion.weight());
            assertThat(actual.get("description").asText()).isEqualTo(criterion.description());

            List<String> sourceReferences = objectMapper.convertValue(
                    actual.get("sourceReferences"), new TypeReference<List<String>>() {
                    });
            assertThat(sourceReferences).containsExactlyElementsOf(criterion.sourceReferences());

            Map<String, String> levels = objectMapper.convertValue(
                    actual.get("levels"), new TypeReference<Map<String, String>>() {
                    });
            assertThat(levels).containsExactlyInAnyOrderEntriesOf(criterion.levels());
        }
    }

    @Test
    void editingOneCopyNeverAffectsAnotherAssignmentCreatedFromTheSameTemplate() throws Exception {
        seedTemplateTestEducator();
        String templateId = templateCatalog.findAll().get(0).id();

        String firstResponse = mockMvc.perform(post("/api/assignments")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssignmentBody(templateId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/assignments")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssignmentBody(templateId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID firstId = UUID.fromString(objectMapper.readTree(firstResponse).get("id").asText());
        UUID secondId = UUID.fromString(objectMapper.readTree(secondResponse).get("id").asText());
        assertThat(firstId).isNotEqualTo(secondId);

        Assignment first = assignmentRepository.findByIdAndEducatorId(firstId, educator.getId()).orElseThrow();
        Assignment second = assignmentRepository.findByIdAndEducatorId(secondId, educator.getId()).orElseThrow();

        List<Long> firstCriterionIds = first.getDraft().getCriteria().stream().map(Criterion::getId).toList();
        List<Long> secondCriterionIds = second.getDraft().getCriteria().stream().map(Criterion::getId).toList();
        assertThat(firstCriterionIds).doesNotContainAnyElementsOf(secondCriterionIds);

        String originalStance = templateCatalog.findById(templateId).orElseThrow().assessmentStance();
        first.setDraftAssessmentStance("Ændret vurderingsgrundlag, kun for denne kopi.");
        assignmentRepository.save(first);

        Assignment secondAfterEdit = assignmentRepository.findByIdAndEducatorId(secondId, educator.getId()).orElseThrow();
        assertThat(secondAfterEdit.getDraft().getAssessmentStance()).isEqualTo(originalStance);
        assertThat(templateCatalog.findById(templateId).orElseThrow().assessmentStance()).isEqualTo(originalStance);
    }

    @Test
    void creatingAnAssignmentFromAnUnknownTemplateIdReturns404() throws Exception {
        seedTemplateTestEducator();

        mockMvc.perform(post("/api/assignments")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssignmentBody("does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("template_not_found"));
    }

    private String createAssignmentBody(String templateId) {
        return """
                {"templateId": "%s"}
                """.formatted(templateId);
    }
}
