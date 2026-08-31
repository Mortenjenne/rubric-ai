package app.rubric;

import app.rubric.json.CriterionResource;
import app.rubric.json.RubricResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the bundled Rubric JSON resource on startup and inserts it if that version is
 * missing. An existing version is never touched: Evaluations recorded against it must
 * keep referring to the Rubric that actually judged them.
 */
@Component
public class RubricSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RubricSeeder.class);
    private static final String BUNDLED_RUBRIC = "rubric/praktikrapport-v1.json";

    private final RubricRepository rubricRepository;
    private final ObjectMapper objectMapper;

    public RubricSeeder(RubricRepository rubricRepository, ObjectMapper objectMapper) {
        this.rubricRepository = rubricRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        seed();
    }

    @Transactional
    public void seed() throws IOException {
        RubricResource resource = readBundledRubric();
        if (rubricRepository.existsById(resource.version())) {
            log.info("Rubric version {} already seeded, leaving it untouched", resource.version());
            return;
        }
        rubricRepository.save(toEntity(resource));
        log.info("Seeded rubric version {} with {} criteria", resource.version(), resource.criteria().size());
    }

    private RubricResource readBundledRubric() throws IOException {
        try (InputStream in = new ClassPathResource(BUNDLED_RUBRIC).getInputStream()) {
            return objectMapper.readValue(in, RubricResource.class);
        }
    }

    private Rubric toEntity(RubricResource resource) {
        Rubric rubric = new Rubric(resource.version(), resource.assignment(), resource.language(), resource.note());
        for (CriterionResource criterionResource : resource.criteria()) {
            Map<String, String> levels = new LinkedHashMap<>(criterionResource.levels());
            rubric.addCriterion(new Criterion(
                    criterionResource.id(),
                    criterionResource.name(),
                    criterionResource.weight(),
                    criterionResource.description(),
                    criterionResource.sourceMaterial(),
                    levels));
        }
        return rubric;
    }
}
