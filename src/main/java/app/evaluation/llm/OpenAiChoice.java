package app.evaluation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One choice in a Chat Completions response. Only the message is read; fields such as
 * {@code finish_reason} and {@code index} are ignored rather than modelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiChoice(OpenAiResponseMessage message) {
}
