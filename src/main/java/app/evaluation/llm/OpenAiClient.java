package app.evaluation.llm;

import app.evaluation.domain.InvalidModelOutputException;
import app.evaluation.domain.SuggestedGradeValue;
import app.rubric.Level;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * The production {@link LlmClient}: calls OpenAI's Chat Completions endpoint with native
 * structured-output enforcement configured on the request, at temperature zero, with a
 * 90-second timeout on the call. The raw message content is handed back exactly as the
 * provider returned it — the schema constrains the shape, but parsing and Bean Validation in
 * {@link app.evaluation.service.EvaluationService} remain the final gate; provider guarantees
 * are never trusted on their own.
 */
@Component
public class OpenAiClient implements LlmClient {

    private static final String BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /**
     * Kept well below {@link #CALL_TIMEOUT} so a connection that never gets established (a
     * black-holed handshake, not a slow response) fails fast instead of quietly spending most
     * of the 90-second budget the spec sets for the whole call before the read timeout ever
     * has a chance to apply.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient;
    private final LlmProperties llmProperties;
    private final OpenAiProperties openAiProperties;
    private final JsonNode responseFormat;

    @Autowired
    public OpenAiClient(LlmProperties llmProperties,
                         OpenAiProperties openAiProperties,
                         ObjectMapper objectMapper,
                         RestClient.Builder restClientBuilder) {
        this(restClientBuilder
                        .baseUrl(BASE_URL)
                        .requestFactory(timeoutRequestFactory())
                        .build(),
                llmProperties, openAiProperties, objectMapper);
    }

    OpenAiClient(RestClient restClient, LlmProperties llmProperties, OpenAiProperties openAiProperties,
                 ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.llmProperties = llmProperties;
        this.openAiProperties = openAiProperties;
        this.responseFormat = buildResponseFormat(objectMapper);
    }

    @Override
    public String call(LlmRequest request) {
        String apiKey = openAiProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set; the OpenAI adapter cannot call the provider without it");
        }

        OpenAiChatRequest body = new OpenAiChatRequest(
                llmProperties.model(),
                0,
                List.of(
                        new OpenAiMessage("system", request.systemPrompt()),
                        new OpenAiMessage("user", request.userPrompt())),
                responseFormat);

        OpenAiChatResponse response = restClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiChatResponse.class);

        String content = response == null ? null : response.firstMessageContent();
        if (content == null || content.isBlank()) {
            throw new InvalidModelOutputException("OpenAI response contained no message content");
        }

        return content;
    }

    private static ClientHttpRequestFactory timeoutRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(CALL_TIMEOUT);
        return factory;
    }

    /**
     * Built from {@link Level} and {@link SuggestedGradeValue} rather than hardcoded, so the
     * enforced schema can never drift from the enums the rest of the service actually parses
     * against.
     */
    private static JsonNode buildResponseFormat(ObjectMapper objectMapper) {
        ArrayNode levelEnum = objectMapper.createArrayNode();
        for (Level level : Level.values()) {
            levelEnum.add(level.label());
        }

        ArrayNode gradeEnum = objectMapper.createArrayNode();
        for (SuggestedGradeValue grade : SuggestedGradeValue.values()) {
            gradeEnum.add(grade.label());
        }

        ObjectNode findingSchema = objectMapper.createObjectNode();
        findingSchema.put("type", "object");
        findingSchema.put("additionalProperties", false);
        ObjectNode findingProperties = findingSchema.putObject("properties");
        findingProperties.set("criterion", stringSchema(objectMapper));
        findingProperties.putObject("level").put("type", "string").set("enum", levelEnum);
        findingProperties.set("strengths", arraySchema(objectMapper, stringSchema(objectMapper)));
        findingProperties.set("weaknesses", arraySchema(objectMapper, stringSchema(objectMapper)));
        findingProperties.set("improvements", arraySchema(objectMapper, stringSchema(objectMapper)));
        findingProperties.set("evidence", arraySchema(objectMapper, stringSchema(objectMapper)));
        ArrayNode findingRequired = objectMapper.createArrayNode();
        findingRequired.add("criterion").add("level").add("strengths").add("weaknesses")
                .add("improvements").add("evidence");
        findingSchema.set("required", findingRequired);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.set("overallAssessment", stringSchema(objectMapper));
        properties.putObject("suggestedGrade").put("type", "string").set("enum", gradeEnum);
        properties.set("findings", arraySchema(objectMapper, findingSchema));
        properties.set("dialogueQuestions", arraySchema(objectMapper, stringSchema(objectMapper)));
        ArrayNode topRequired = objectMapper.createArrayNode();
        topRequired.add("overallAssessment").add("suggestedGrade").add("findings").add("dialogueQuestions");
        schema.set("required", topRequired);

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "evaluation");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);

        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");
        responseFormat.set("json_schema", jsonSchema);
        return responseFormat;
    }

    private static ObjectNode stringSchema(ObjectMapper objectMapper) {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private static ObjectNode arraySchema(ObjectMapper objectMapper, JsonNode items) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "array");
        schema.set("items", items);
        return schema;
    }
}
