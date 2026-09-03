package app.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level: exercises the real classpath resources under {@code src/main/resources/templates},
 * so a malformed bundled Template JSON file fails here rather than only at startup.
 */
class TemplateCatalogTest {

    private final TemplateCatalog catalog = new TemplateCatalog(new ObjectMapper());

    @Test
    void loadsBothBundledTemplates() {
        List<Template> templates = catalog.findAll();

        assertThat(templates).extracting(Template::id)
                .containsExactlyInAnyOrder("praktikrapport", "skriftlig-aflevering");
    }

    @Test
    void everyTemplateCarriesATitleDescriptionStanceAndAtLeastOneCriterion() {
        for (Template template : catalog.findAll()) {
            assertThat(template.title()).isNotBlank();
            assertThat(template.description()).isNotBlank();
            assertThat(template.assessmentStance()).isNotBlank();
            assertThat(template.criteria()).isNotEmpty();
        }
    }

    @Test
    void praktikrapportKeepsItsReadableCriterionKeysAndCalibrationStance() {
        Template template = catalog.findById("praktikrapport").orElseThrow();

        assertThat(template.criteria()).extracting(TemplateCriterion::key)
                .containsExactly("formkrav", "viden", "faerdigheder", "kompetencer", "refleksion", "dare-share-care");
        assertThat(template.assessmentStance()).contains("erhvervsakademiuddannelse");
        assertThat(template.assessmentStance()).contains("Anfør aldrig fraværet af sådanne nøgleord");
    }

    @Test
    void unknownTemplateIdIsAbsent() {
        Optional<Template> template = catalog.findById("does-not-exist");

        assertThat(template).isEmpty();
    }
}
