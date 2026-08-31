package app.evaluation.llm;

import app.evaluation.domain.InvalidModelOutputException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit-level: exercises only the wire format the adapter builds and reads, against a mocked
 * HTTP server rather than the real OpenAI endpoint — this is not the {@link LlmClient}
 * substitution seam the spec's integration tests use, just a fast, network-free check on this
 * one adapter's own request/response translation.
 */
class OpenAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmProperties llmProperties = new LlmProperties("openai", "gpt-5.6-luna");
    private final OpenAiProperties testKey = new OpenAiProperties("test-key");
    private final OpenAiProperties noKey = new OpenAiProperties("");

    @Test
    void callsChatCompletionsWithStructuredOutputAtTemperatureZeroAndReturnsRawContent() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String rawPayload = "{\"overallAssessment\":\"x\"}";
        String responseBody = """
                { "choices": [ { "message": { "content": %s } } ] }
                """.formatted(objectMapper.writeValueAsString(rawPayload));

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
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

        OpenAiClient client = new OpenAiClient(builder.build(), llmProperties, testKey, objectMapper);

        String result = client.call(new LlmRequest("system prompt", "user prompt"));

        assertThat(result).isEqualTo(rawPayload);
        server.verify();
    }

    @Test
    void missingApiKeyFailsFastWithoutCallingTheProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(builder.build(), llmProperties, noKey, objectMapper);

        assertThatThrownBy(() -> client.call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(IllegalStateException.class);

        server.verify();
    }

    @Test
    void emptyMessageContentIsTreatedAsInvalidModelOutput() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess("""
                        { "choices": [ { "message": { "content": "" } } ] }
                        """, MediaType.APPLICATION_JSON));

        OpenAiClient client = new OpenAiClient(builder.build(), llmProperties, testKey, objectMapper);

        assertThatThrownBy(() -> client.call(new LlmRequest("system prompt", "user prompt")))
                .isInstanceOf(InvalidModelOutputException.class);
    }
}
