package app.evaluation.llm;

import app.evaluation.domain.InvalidModelOutputException;
import app.evaluation.domain.LlmConfigurationException;
import app.evaluation.domain.RateLimitedException;
import app.evaluation.domain.UpstreamUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit-level: exercises only the wire format the adapter builds and reads, against a mocked
 * HTTP server rather than the real OpenAI endpoint — this is not the {@link LlmClient}
 * substitution seam the spec's integration tests use, just a fast, network-free check on this
 * one adapter's own request/response translation.
 */
class OpenAiClientTest {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmProperties llmProperties = new LlmProperties("openai", "gpt-5.6-luna");
    private final OpenAiProperties testKey = new OpenAiProperties("test-key");
    private final OpenAiProperties noKey = new OpenAiProperties("");

    private record ServerAndClient(MockRestServiceServer server, OpenAiClient client) {
    }

    /**
     * Every test needs the same pair: a mock server bound to the base URL the adapter targets,
     * and a client wired to it and to the given credential. The no-op backoff built into this
     * test constructor (see {@link OpenAiClient}) is what keeps the retry tests fast.
     */
    private ServerAndClient mockOpenAi(OpenAiProperties credential) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(builder.build(), llmProperties, credential, objectMapper);
        return new ServerAndClient(server, client);
    }

    private ServerAndClient mockOpenAi() {
        return mockOpenAi(testKey);
    }

    @Test
    void callsChatCompletionsWithStructuredOutputAtTemperatureZeroAndReturnsRawContent() throws Exception {
        ServerAndClient mock = mockOpenAi();

        String rawPayload = "{\"overallAssessment\":\"x\"}";
        String responseBody = """
                { "choices": [ { "message": { "content": %s } } ] }
                """.formatted(objectMapper.writeValueAsString(rawPayload));

        mock.server().expect(requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.6-luna"))
                .andExpect(jsonPath("$.temperature").value(0))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("system prompt"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("user prompt"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.additionalProperties").value(false))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.suggestedGrade.enum")
                        .value(org.hamcrest.Matchers.hasItem("10")))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String result = mock.client().call(new LlmRequest("system prompt", "user prompt"));

        assertThat(result).isEqualTo(rawPayload);
        mock.server().verify();
    }

    @Test
    void missingApiKeyFailsFastWithoutCallingTheProvider() {
        ServerAndClient mock = mockOpenAi(noKey);

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(LlmConfigurationException.class);

        mock.server().verify();
    }

    @Test
    void rateLimitedResponseIsRetriedThreeTimesThenFailsWithRateLimited() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(times(3), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(RateLimitedException.class);

        mock.server().verify();
    }

    @Test
    void serverErrorIsRetriedThreeTimesThenFailsWithUpstreamUnavailable() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(times(3), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(UpstreamUnavailableException.class);

        mock.server().verify();
    }

    @Test
    void timeoutIsRetriedThreeTimesThenFailsWithUpstreamUnavailable() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(times(3), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new HttpTimeoutException("request timed out");
                });

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(UpstreamUnavailableException.class);

        mock.server().verify();
    }

    @Test
    void refusedConnectionFailsFastWithoutRetrying() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(once(), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ConnectException("Connection refused");
                });

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(UpstreamUnavailableException.class);

        mock.server().verify();
    }

    @Test
    void unauthorizedResponseFailsImmediatelyAsAConfigurationFaultWithoutRetrying() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(once(), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(LlmConfigurationException.class);

        mock.server().verify();
    }

    @Test
    void badRequestResponseFailsImmediatelyAsAConfigurationFaultWithoutRetrying() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(once(), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(LlmConfigurationException.class);

        mock.server().verify();
    }

    @Test
    void otherClientErrorResponseFailsImmediatelyAsAConfigurationFaultWithoutRetrying() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(once(), requestTo(CHAT_COMPLETIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(LlmConfigurationException.class);

        mock.server().verify();
    }

    @Test
    void emptyMessageContentIsTreatedAsInvalidModelOutput() {
        ServerAndClient mock = mockOpenAi();

        mock.server().expect(requestTo(CHAT_COMPLETIONS_URL))
                .andRespond(withSuccess("""
                        { "choices": [ { "message": { "content": "" } } ] }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> mock.client().call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(InvalidModelOutputException.class);
    }
}
