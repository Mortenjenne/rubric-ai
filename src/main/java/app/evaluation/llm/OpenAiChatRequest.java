package app.evaluation.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * The Chat Completions request body as OpenAI expects it on the wire.
 */
record OpenAiChatRequest(
        String model,
        int temperature,
        List<OpenAiMessage> messages,
        @JsonProperty("response_format") JsonNode responseFormat) {
}
