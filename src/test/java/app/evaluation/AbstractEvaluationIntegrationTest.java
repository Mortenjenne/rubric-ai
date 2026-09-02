package app.evaluation;

import app.evaluation.persistence.EvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers Postgres and MockMvc/repository wiring for the evaluation feature's
 * integration tests, which all drive the real HTTP endpoint against a real Postgres.
 *
 * <p>The container is started once, manually, rather than through {@code @Testcontainers}: that
 * extension stops a static {@code @Container} field in {@code afterAll}, and since this field is
 * inherited, "after all" means after whichever subclass happens to run last among
 * {@link EvaluationByIdIntegrationTest}, {@link EvaluationIntegrationTest} and
 * {@link EvaluationListIntegrationTest} — leaving the ones after it pointed at a stopped
 * container's dead port. A singleton started in a static initializer and left running for Ryuk to
 * reap at JVM exit has no "last" class to race.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractEvaluationIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected EvaluationRepository evaluationRepository;
}
