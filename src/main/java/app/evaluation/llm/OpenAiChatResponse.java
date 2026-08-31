package app.evaluation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Chat Completions response, deserialised only as far as the raw message content
 * {@link OpenAiClient} hands onward to parsing and validation. Unknown fields (usage,
 * system_fingerprint, ...) are ignored rather than modelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChatResponse(List<OpenAiChoice> choices) {

    /**
     * @return the first choice's message content, or {@code null} if the response carries no
     * choice or that choice carries no content — the caller decides how to treat that.
     */
    String firstMessageContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        OpenAiResponseMessage message = choices.get(0).message();
        return message == null ? null : message.content();
    }
}
