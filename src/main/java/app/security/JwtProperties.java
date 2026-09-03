package app.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The JWT signing secret is bound purely from the {@code JWT_SECRET} environment variable via
 * Spring's relaxed binding (the same pattern {@code OpenAiProperties} uses for
 * {@code OPENAI_API_KEY}) — nothing in {@code application.yml} names it, so there is no
 * committed default to accidentally leave in place. The compact constructor fails startup
 * outright when it is missing: a YAML placeholder like {@code ${JWT_SECRET}} with no default
 * would otherwise bind the literal, unresolved placeholder text instead of failing, which is a
 * signing key nobody chose. {@code toString} is overridden so a stray log statement can never
 * leak the secret itself.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable must be set; refusing to start without a signing secret");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=****, expiration=%s]".formatted(expiration);
    }
}
