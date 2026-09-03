package app.security;

import app.evaluation.AbstractEvaluationIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real {@code /api/auth/login} endpoint against a real Postgres (Testcontainers),
 * reusing the evaluation feature's shared Testcontainers wiring rather than starting a second
 * container.
 */
class AuthIntegrationTest extends AbstractEvaluationIntegrationTest {

    private static final String EMAIL = "login-test@example.com";
    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private ObjectMapper objectMapper;

    private void seedLoginEducator() {
        seedEducator(EMAIL, "Login Test Educator", PASSWORD);
    }

    private String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    @Test
    void correctCredentialsReturnAJwtThatWorksOnAProtectedEndpoint() throws Exception {
        seedLoginEducator();

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailReturnTheSame401() throws Exception {
        seedLoginEducator();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "the-wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody-at-all@example.com", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void repeatedFailuresForOneEmailAreEventuallyThrottled() throws Exception {
        String throttledEmail = "throttle-test@example.com";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(throttledEmail, "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(throttledEmail, "wrong-password")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_attempts"));
    }

    @Test
    void loginNeedsNoTokenItself() throws Exception {
        seedLoginEducator();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void aProtectedEndpointWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void aProtectedEndpointWithAGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/evaluations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }
}
