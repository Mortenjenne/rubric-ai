package app.evaluation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The message inside one choice of a Chat Completions response. Only {@code content} is read;
 * every other field OpenAI sends (refusal, annotations, ...) is ignored rather than modelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OpenAiResponseMessage(String content) {
}
