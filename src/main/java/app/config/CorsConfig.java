package app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the CORS policy for {@code /api/**}, sourced from {@link CorsProperties} so the
 * allowed origins can differ between local development and a deployed environment without a
 * code change.
 *
 * <p>Credentials are deliberately left disabled: CORS "credentials" means cookies and
 * browser-managed HTTP auth, neither of which this API uses — the bearer token on {@code
 * /api/**} is a header the frontend sets itself, which {@code allowedHeaders("*")} already
 * permits cross-origin without needing {@code allowCredentials}. Leaving it off also means the
 * wildcard-origin-with-credentials combination Spring rejects at startup cannot arise. Enabling
 * credentials later (e.g. for cookies) means revisiting this policy on purpose, not by accident.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(new String[0]))
                // Only what the API actually exposes: POST to evaluate, GET to list and fetch.
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
