package app.educator;

import app.educator.EducatorsProperties.SeededEducator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Creates Educator accounts from the {@code app.educators} configuration list at startup.
 * There is no registration endpoint, so this is the only way an account comes to exist. An
 * existing account is never updated: whoever changes a seeded password does so directly against
 * the database, not by editing configuration and restarting.
 */
@Component
public class EducatorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EducatorSeeder.class);

    /** Bcrypt's own effective limit is 72 bytes; this is a floor against trivially weak seeds. */
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final EducatorRepository educatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EducatorsProperties educatorsProperties;
    private final Clock clock;

    public EducatorSeeder(EducatorRepository educatorRepository,
                           PasswordEncoder passwordEncoder,
                           EducatorsProperties educatorsProperties,
                           Clock clock) {
        this.educatorRepository = educatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.educatorsProperties = educatorsProperties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        List<SeededEducator> configured = educatorsProperties.educators();
        if (configured == null) {
            return;
        }
        for (SeededEducator seeded : configured) {
            seedOne(seeded);
        }
    }

    private void seedOne(SeededEducator seeded) {
        if (seeded.password() == null || seeded.password().isBlank()) {
            log.warn("No password configured for educator {}, skipping seed so the rest of the "
                    + "environment can still boot", seeded.email());
            return;
        }
        if (seeded.password().length() < MIN_PASSWORD_LENGTH) {
            log.warn("Password configured for educator {} is shorter than the required {} "
                    + "characters, skipping seed", seeded.email(), MIN_PASSWORD_LENGTH);
            return;
        }
        if (educatorRepository.existsByEmail(seeded.email())) {
            log.info("Educator {} already seeded, leaving it untouched", seeded.email());
            return;
        }
        Educator educator = new Educator(
                UUID.randomUUID(),
                seeded.email(),
                seeded.displayName(),
                passwordEncoder.encode(seeded.password()),
                Instant.now(clock));
        educatorRepository.save(educator);
        log.info("Seeded educator {}", seeded.email());
    }
}
