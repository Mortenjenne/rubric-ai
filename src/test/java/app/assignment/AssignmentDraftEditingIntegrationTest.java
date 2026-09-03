package app.assignment;

import app.educator.Educator;
import app.evaluation.AbstractEvaluationIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real {@code GET/PUT/DELETE /api/assignments} endpoints against a real Postgres
 * (Testcontainers), reusing the evaluation feature's shared wiring. Uses its own Educator rather
 * than {@link #TEST_EDUCATOR_EMAIL} — see {@link AssignmentFromTemplateIntegrationTest} for why.
 */
class AssignmentDraftEditingIntegrationTest extends AbstractEvaluationIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    private Educator educator;
    private String authorization;

    /**
     * Each test gets its own Educator (rather than one shared constant) — Postgres is shared
     * across every test method in this class, and a fixed email would let one test's Assignments
     * leak into another's listing assertions.
     */
    private void seedDraftEditingEducator() {
        String email = "draft-editing-educator-" + UUID.randomUUID() + "@example.com";
        educator = seedEducator(email, "Draft Editing Educator", "irrelevant-test-password");
        authorization = authorizationHeaderFor(educator);
    }

    private UUID createAssignmentFromPraktikrapportTemplate() throws Exception {
        String response = mockMvc.perform(post("/api/assignments")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": "praktikrapport"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    @Test
    void listingAssignmentsExcludesSoftDeletedOnesAndReportsPublishedVersionStatus() throws Exception {
        seedDraftEditingEducator();
        UUID keptId = createAssignmentFromPraktikrapportTemplate();
        UUID deletedId = createAssignmentFromPraktikrapportTemplate();

        mockMvc.perform(delete("/api/assignments/" + deletedId)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/assignments")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(keptId.toString()))
                .andExpect(jsonPath("$[0].hasPublishedVersion").value(false))
                .andExpect(jsonPath("$[0].latestVersionNumber").doesNotExist())
                .andExpect(jsonPath("$[0].lastEditedAt").isNotEmpty());
    }

    @Test
    void gettingOneAssignmentReturnsItsDraftAndPublishedVersions() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        mockMvc.perform(get("/api/assignments/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Praktikrapport, 5. semester, datamatikeruddannelsen"))
                .andExpect(jsonPath("$.draft.criteria", hasSize(6)))
                .andExpect(jsonPath("$.versions", hasSize(0)));
    }

    @Test
    void replacingTheDraftRenamesKeepsExistingKeysAssignsNewKeysAndDropsOmittedCriteria() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        String replaceBody = """
                {
                  "title": "Praktikrapport (revideret)",
                  "assessmentStance": "En ny, kortere holdning.",
                  "criteria": [
                    {
                      "key": "formkrav",
                      "name": "Formkrav (omdøbt)",
                      "weight": 12,
                      "description": "Ny beskrivelse.",
                      "sourceReferences": ["krav-til-rapport.md"],
                      "levels": {
                        "Udmærket": "u",
                        "Tilfredsstillende": "t",
                        "Acceptabelt": "a",
                        "Mangelfuldt": "m"
                      }
                    },
                    {
                      "name": "Helt ny kriterie",
                      "weight": 5,
                      "description": "En kriterie uden nøgle.",
                      "sourceReferences": [],
                      "levels": {}
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replaceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Praktikrapport (revideret)"))
                .andExpect(jsonPath("$.draft.assessmentStance").value("En ny, kortere holdning."))
                .andExpect(jsonPath("$.draft.criteria", hasSize(2)))
                .andExpect(jsonPath("$.draft.criteria[0].key").value("formkrav"))
                .andExpect(jsonPath("$.draft.criteria[0].name").value("Formkrav (omdøbt)"))
                .andExpect(jsonPath("$.draft.criteria[0].weight").value(12))
                .andExpect(jsonPath("$.draft.criteria[1].key").value("c1"))
                .andExpect(jsonPath("$.draft.criteria[1].name").value("Helt ny kriterie"));

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getDraft().getCriteria()).hasSize(2);
        assertThat(persisted.getUpdatedAt()).isAfterOrEqualTo(persisted.getCreatedAt());
    }

    @Test
    void replacingTheDraftSavesAHalfWrittenRubricWithoutAnyValidation() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        String halfWrittenBody = """
                {
                  "title": "",
                  "criteria": [
                    { "name": "", "weight": 0, "sourceReferences": [], "levels": {} }
                  ]
                }
                """;

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(halfWrittenBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(""))
                .andExpect(jsonPath("$.draft.assessmentStance").value(""))
                .andExpect(jsonPath("$.draft.criteria", hasSize(1)))
                .andExpect(jsonPath("$.draft.criteria[0].key").value("c1"))
                .andExpect(jsonPath("$.draft.criteria[0].name").value(""));
    }

    @Test
    void replacingTheDraftNeverTouchesAPublishedVersion() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        Assignment beforeEdit = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        beforeEdit.publishVersion(Instant.now());
        assignmentRepository.save(beforeEdit);

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Anden titel",
                                  "assessmentStance": "Anden holdning",
                                  "criteria": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.criteria", hasSize(0)));

        String getResponse = mockMvc.perform(get("/api/assignments/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions", hasSize(1)))
                .andExpect(jsonPath("$.versions[0].versionNumber").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(getResponse);
        assertThat(root.get("draft").get("criteria")).isEmpty();

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getVersions()).hasSize(1);
        assertThat(persisted.getVersions().get(0).getCriteria()).hasSize(6);
        assertThat(persisted.getVersions().get(0).getAssessmentStance())
                .isNotEqualTo("Anden holdning");
    }

    @Test
    void deletingAnAssignmentSoftDeletesItButKeepsItsRow() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        mockMvc.perform(delete("/api/assignments/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.isDeleted()).isTrue();
    }

    @Test
    void everyEndpointReturns404NotFoundForAnotherEducatorsAssignment() throws Exception {
        seedDraftEditingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        Educator otherEducator = seedEducator("other-draft-editing-educator@example.com",
                "Other Draft Editing Educator", "irrelevant-test-password");
        String otherAuthorization = authorizationHeaderFor(otherEducator);

        mockMvc.perform(get("/api/assignments/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assignment_not_found"));

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Kapret", "assessmentStance": "", "criteria": []}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assignment_not_found"));

        mockMvc.perform(delete("/api/assignments/" + id)
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assignment_not_found"));
    }
}
