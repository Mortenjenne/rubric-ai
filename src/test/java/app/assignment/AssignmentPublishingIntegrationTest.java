package app.assignment;

import app.educator.Educator;
import app.evaluation.AbstractEvaluationIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real {@code POST /api/assignments/{id}/versions} endpoint against a real Postgres
 * (Testcontainers), reusing the evaluation feature's shared wiring. Uses its own Educator rather
 * than {@link #TEST_EDUCATOR_EMAIL} — see {@link AssignmentFromTemplateIntegrationTest} for why.
 */
class AssignmentPublishingIntegrationTest extends AbstractEvaluationIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    private Educator educator;
    private String authorization;

    private void seedPublishingEducator() {
        String email = "publishing-educator-" + UUID.randomUUID() + "@example.com";
        educator = seedEducator(email, "Publishing Educator", "irrelevant-test-password");
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

    private String fullyDescribedCriterion(String key, String name, int weight) {
        return """
                {
                  "key": "%s",
                  "name": "%s",
                  "weight": %d,
                  "description": "En beskrivelse.",
                  "sourceReferences": [],
                  "levels": {
                    "Udmærket": "u",
                    "Tilfredsstillende": "t",
                    "Acceptabelt": "a",
                    "Mangelfuldt": "m"
                  }
                }
                """.formatted(key, name, weight);
    }

    @Test
    void publishingSnapshotsAWeightsNotSummingTo100AndABlankStanceIntoAFrozenNumberedVersion() throws Exception {
        seedPublishingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        String replaceBody = """
                {
                  "title": "Matematik, opgave 3",
                  "assessmentStance": "",
                  "criteria": [%s, %s]
                }
                """.formatted(fullyDescribedCriterion("c1", "Metode", 5),
                fullyDescribedCriterion("c2", "Facit", 5));

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replaceBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.title").value("Matematik, opgave 3"))
                .andExpect(jsonPath("$.assessmentStance").value(""))
                .andExpect(jsonPath("$.criteria", hasSize(2)))
                .andExpect(jsonPath("$.criteria[0].key").value("c1"))
                .andExpect(jsonPath("$.criteria[1].key").value("c2"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2));

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getVersions()).hasSize(2);
        assertThat(persisted.getVersions().get(0).getVersionNumber()).isEqualTo(1);
        assertThat(persisted.getVersions().get(0).getTitle()).isEqualTo("Matematik, opgave 3");
        assertThat(persisted.getVersions().get(1).getVersionNumber()).isEqualTo(2);
        assertThat(persisted.getDraft().getCriteria()).hasSize(2);
        assertThat(persisted.getDraft().getAssessmentStance()).isEmpty();
    }

    @Test
    void publishingRejectsWithEveryValidationFailureAtOnce() throws Exception {
        seedPublishingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        String incompleteCriterion = """
                {
                  "key": "dup",
                  "name": "Ufuldstændig",
                  "weight": 1,
                  "description": "En beskrivelse.",
                  "sourceReferences": [],
                  "levels": {
                    "Udmærket": "u",
                    "Tilfredsstillende": "",
                    "Acceptabelt": "a"
                  }
                }
                """;
        String duplicateKeyCriterion = fullyDescribedCriterion("dup", "Duplikat", 1);

        String replaceBody = """
                {
                  "title": "Ufuldstændigt udkast",
                  "assessmentStance": "",
                  "criteria": [%s, %s]
                }
                """.formatted(incompleteCriterion, duplicateKeyCriterion);

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replaceBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("draft_invalid"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors", hasItem(containsString("Tilfredsstillende"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("Mangelfuldt"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("dup"))));

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getVersions()).isEmpty();
    }

    @Test
    void publishingWithNoCriteriaAtAllIsRejected() throws Exception {
        seedPublishingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Tomt udkast", "assessmentStance": "", "criteria": []}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("draft_invalid"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("no criteria"))));
    }

    @Test
    void editingTheDraftAfterPublishingLeavesThePublishedVersionUntouched() throws Exception {
        seedPublishingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.criteria", hasSize(6)));

        mockMvc.perform(put("/api/assignments/" + id + "/draft")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Efter udgivelse",
                                  "assessmentStance": "En helt anden holdning",
                                  "criteria": [%s]
                                }
                                """.formatted(fullyDescribedCriterion("c1", "Nyt kriterie", 42))))
                .andExpect(status().isOk());

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getVersions()).hasSize(1);
        AssignmentVersion publishedVersion = persisted.getVersions().get(0);
        assertThat(publishedVersion.getCriteria()).hasSize(6);
        assertThat(publishedVersion.getTitle()).isEqualTo("Praktikrapport, 5. semester, datamatikeruddannelsen");
        assertThat(publishedVersion.getAssessmentStance()).isNotEqualTo("En helt anden holdning");
        assertThat(persisted.getTitle()).isEqualTo("Efter udgivelse");
        assertThat(persisted.getDraft().getCriteria()).hasSize(1);
        assertThat(persisted.getDraft().getCriteria().get(0).getName()).isEqualTo("Nyt kriterie");
        assertThat(persisted.getDraft().getAssessmentStance()).isEqualTo("En helt anden holdning");
    }

    @Test
    void publishingAnAssignmentOwnedByAnotherEducatorReturns404() throws Exception {
        seedPublishingEducator();
        UUID id = createAssignmentFromPraktikrapportTemplate();

        Educator otherEducator = seedEducator("other-publishing-educator@example.com",
                "Other Publishing Educator", "irrelevant-test-password");
        String otherAuthorization = authorizationHeaderFor(otherEducator);

        mockMvc.perform(post("/api/assignments/" + id + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assignment_not_found"));

        Assignment persisted = assignmentRepository.findByIdAndEducatorId(id, educator.getId()).orElseThrow();
        assertThat(persisted.getVersions()).isEmpty();
    }
}
