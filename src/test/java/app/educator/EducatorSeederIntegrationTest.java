package app.educator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real Spring context against a real Postgres (Testcontainers), exactly as the
 * service would on startup, and asserts the seeded accounts by reading them back from the
 * database. The configured list below exercises every seeding rule in one boot: a normal
 * account, one with no password configured, and one whose password is too short.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class EducatorSeederIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void educatorProperties(DynamicPropertyRegistry registry) {
        registry.add("app.educators[0].email", () -> "seeded@example.com");
        registry.add("app.educators[0].display-name", () -> "Seeded Educator");
        registry.add("app.educators[0].password", () -> "a-sufficiently-long-password");

        registry.add("app.educators[1].email", () -> "no-password@example.com");
        registry.add("app.educators[1].display-name", () -> "No Password Educator");
        registry.add("app.educators[1].password", () -> "");

        registry.add("app.educators[2].email", () -> "too-short@example.com");
        registry.add("app.educators[2].display-name", () -> "Too Short Educator");
        registry.add("app.educators[2].password", () -> "short");
    }

    @Autowired
    private EducatorRepository educatorRepository;

    @Autowired
    private EducatorSeeder educatorSeeder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsAnAccountForAnEntryWithAValidPassword() {
        Optional<Educator> found = educatorRepository.findByEmail("seeded@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Seeded Educator");
        assertThat(passwordEncoder.matches("a-sufficiently-long-password", found.get().getPasswordHash())).isTrue();
    }

    @Test
    void skipsAnEntryWithNoPasswordConfigured() {
        assertThat(educatorRepository.findByEmail("no-password@example.com")).isEmpty();
    }

    @Test
    void skipsAnEntryWhosePasswordIsShorterThanTheMinimumLength() {
        assertThat(educatorRepository.findByEmail("too-short@example.com")).isEmpty();
    }

    @Test
    void aSecondSeedLeavesAnExistingAccountUntouched() {
        // The seeder already ran once as an ApplicationRunner while the context booted.
        Educator before = educatorRepository.findByEmail("seeded@example.com").orElseThrow();
        String hashBefore = before.getPasswordHash();

        educatorSeeder.seed();

        assertThat(educatorRepository.count()).isEqualTo(1);
        Educator after = educatorRepository.findByEmail("seeded@example.com").orElseThrow();
        assertThat(after.getPasswordHash()).isEqualTo(hashBefore);
    }
}
