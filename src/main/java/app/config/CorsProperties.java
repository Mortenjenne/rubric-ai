package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origins allowed to call the API from a browser. The frontend runs on a separate origin during
 * development (Vite's default port), so without this the browser's CORS preflight blocks every
 * request before it reaches a controller.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {
}
