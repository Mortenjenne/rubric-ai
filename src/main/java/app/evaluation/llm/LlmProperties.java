package app.evaluation.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which provider and model produced an Evaluation — recorded on every one, so a difference
 * between two runs of the same Submission is explainable rather than mysterious.
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(String provider, String model) {
}
