package app.evaluation.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The OpenAI credential, bound from the environment via Spring's relaxed binding (the
 * {@code OPENAI_API_KEY} environment variable maps onto {@code openai.api-key}). Never
 * committed, and never present in any bundled configuration file. {@code toString} is
 * overridden so a stray log statement or exception message that prints this object can never
 * leak the key.
 */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey) {

    @Override
    public String toString() {
        return "OpenAiProperties[apiKey=%s]".formatted(apiKey == null || apiKey.isBlank() ? "(unset)" : "****");
    }
}
