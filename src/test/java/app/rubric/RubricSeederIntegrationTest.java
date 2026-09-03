package app.rubric;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real Spring context against a real Postgres (Testcontainers), exactly as the
 * service would on startup, and asserts the seeded Rubric by reading it back from the database.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class RubricSeederIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RubricRepository rubricRepository;

    @Autowired
    private RubricSeeder rubricSeeder;

    @Test
    void seedsRubricVersionOneOnStartup() {
        // The seeder already ran once as an ApplicationRunner while the context booted.
        Optional<Rubric> found = rubricRepository.findById(1);
        assertThat(found).isPresent();

        Rubric rubric = found.get();
        assertThat(rubric.getAssignment()).isEqualTo("Praktikrapport, 5. semester, datamatikeruddannelsen");
        assertThat(rubric.getLanguage()).isEqualTo("da");
        assertThat(rubric.getCriteria()).hasSize(6);

        List<Criterion> criteria = rubric.getCriteria();
        assertThat(criteria).extracting(Criterion::getKey)
                .containsExactly("formkrav", "viden", "faerdigheder", "kompetencer", "refleksion", "dare-share-care");

        assertThat(criteria.stream().mapToInt(Criterion::getWeight).sum()).isEqualTo(100);

        Criterion faerdigheder = criteria.get(2);
        assertThat(faerdigheder.getName()).isEqualTo("Færdigheder i praksis");
        assertThat(faerdigheder.getWeight()).isEqualTo(25);
        assertThat(faerdigheder.getSourceMaterial()).containsExactly("laeringsmaal.md");
        assertThat(faerdigheder.getDescription()).contains("praksisnære problemstillinger");

        assertThat(faerdigheder.getLevels()).containsOnlyKeys(
                "Mangelfuldt", "Acceptabelt", "Tilfredsstillende", "Udmærket");
        assertThat(faerdigheder.getLevels().get("Udmærket"))
                .contains("Alle fire færdighedsmål er belagt med konkrete opgaver fra praktikken.");

        Criterion dareShareCare = criteria.get(5);
        assertThat(dareShareCare.getName()).isEqualTo("Dare, Share, Care");
        assertThat(dareShareCare.getLevels().get("Mangelfuldt"))
                .isEqualTo("Værdierne er hverken navngivet eller genkendelige i rapportens indhold.");
    }

    @Test
    void secondBootLeavesTheSeededRubricUntouched() throws Exception {
        Rubric before = rubricRepository.findById(1).orElseThrow();
        String descriptionBefore = before.getCriteria().get(0).getDescription();
        Map<String, String> levelsBefore = Map.copyOf(before.getCriteria().get(0).getLevels());

        // Simulate a second startup against the already-seeded database.
        rubricSeeder.seed();

        assertThat(rubricRepository.count()).isEqualTo(1);
        Rubric after = rubricRepository.findById(1).orElseThrow();
        assertThat(after.getCriteria()).hasSize(6);
        assertThat(after.getCriteria().get(0).getDescription()).isEqualTo(descriptionBefore);
        assertThat(after.getCriteria().get(0).getLevels()).isEqualTo(levelsBefore);
    }
}
