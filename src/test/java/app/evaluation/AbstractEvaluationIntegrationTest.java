package app.evaluation;

import app.educator.Educator;
import app.educator.EducatorRepository;
import app.evaluation.persistence.EvaluationRepository;
import app.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

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
@ActiveProfiles("test")
public abstract class AbstractEvaluationIntegrationTest {

    protected static final String TEST_EDUCATOR_EMAIL = "test-educator@example.com";

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

    @Autowired
    protected EducatorRepository educatorRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    protected Clock clock;

    /**
     * The {@code Authorization} header value for a seeded test Educator, created directly
     * through the repository (not through {@code EducatorSeeder}, which only reads from
     * configuration) the first time a test in the class asks for it.
     */
    protected String authorizationHeader() {
        return authorizationHeaderFor(seedEducator(TEST_EDUCATOR_EMAIL, "Test Educator",
                "irrelevant-test-password"));
    }

    /**
     * The {@code Authorization} header value for an arbitrary seeded Educator — for a test that
     * must not create Assignments under {@link #TEST_EDUCATOR_EMAIL}, since the Postgres
     * container (and so that account's Assignment history) is shared across every integration
     * test class, and {@link app.evaluation.service.EvaluationService}'s temporary seam picks
     * that Educator's most recently created Assignment.
     */
    protected String authorizationHeaderFor(Educator educator) {
        return "Bearer " + jwtService.issue(educator.getId());
    }

    /**
     * Creates an Educator directly through the repository, or returns the existing one for this
     * email — the one seeding path every integration test that needs a real account shares,
     * rather than each rolling its own.
     */
    protected Educator seedEducator(String email, String displayName, String password) {
        return educatorRepository.findByEmail(email)
                .orElseGet(() -> educatorRepository.save(new Educator(
                        UUID.randomUUID(), email, displayName,
                        passwordEncoder.encode(password), Instant.now(clock))));
    }
}
