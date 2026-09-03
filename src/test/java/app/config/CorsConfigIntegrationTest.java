package app.config;

import app.evaluation.AbstractEvaluationIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The frontend runs on a different origin during development (Vite's default port), so the
 * browser sends a CORS preflight before every request. Without a policy allowing that origin,
 * Spring answers the preflight with no Access-Control-Allow-Origin header and the browser blocks
 * the real request before it ever reaches {@link app.evaluation.web.EvaluationController}.
 *
 * <p>The browser checks the header on both halves of the exchange, so the preflight and the
 * actual response are asserted separately. Reuses the evaluation feature's shared Testcontainers
 * wiring — the policy is exercised through real handler mapping, and the actual (non-preflight)
 * request needs a bearer token now that every {@code /api/**} endpoint requires one.
 */
class CorsConfigIntegrationTest extends AbstractEvaluationIntegrationTest {

    @Test
    void preflightFromTheConfiguredFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/evaluations")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void theLoopbackAddressIsAllowedToo() throws Exception {
        mockMvc.perform(options("/api/evaluations")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"));
    }

    @Test
    void theActualResponseAlsoCarriesTheAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/evaluations")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    /**
     * Security's 401 is written by {@link app.security.web.JwtAuthenticationEntryPoint} before
     * the request ever reaches Spring MVC's own CORS handling, so the header has to come from
     * Security's own CORS support (enabled in {@link app.security.SecurityConfig}) or the
     * browser would block the caller from reading this response too.
     */
    @Test
    void anUnauthenticatedRejectionStillCarriesTheAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/evaluations")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/evaluations")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
