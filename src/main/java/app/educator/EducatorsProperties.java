package app.educator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * The Educator accounts to seed at startup, bound from the {@code app.educators} list the spec
 * names. The prefix here is the parent {@code app} rather than {@code app.educators} itself
 * (unlike this project's other {@code @ConfigurationProperties} records, which each bind their
 * own exact prefix): {@code app.educators} must resolve directly to a list, and Spring cannot
 * bind {@code @ConfigurationProperties} straight onto a {@code List} — it needs one named
 * component, which {@link #educators()} supplies.
 *
 * <p>Email and display name are ordinary configuration; each entry's password is meant to be an
 * environment-variable placeholder (e.g. {@code ${EDUCATOR1_PASSWORD:}}) so no credential is
 * ever committed. {@code toString} is overridden so a stray log statement can never leak a
 * password.
 */
@ConfigurationProperties(prefix = "app")
public record EducatorsProperties(List<SeededEducator> educators) {

    public record SeededEducator(String email, String displayName, String password) {

        @Override
        public String toString() {
            return "SeededEducator[email=%s, displayName=%s, password=%s]"
                    .formatted(email, displayName, password == null || password.isBlank() ? "(unset)" : "****");
        }
    }
}
