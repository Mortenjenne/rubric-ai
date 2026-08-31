package app.evaluation;

import app.evaluation.llm.FakeLlmClient;
import app.evaluation.llm.LlmClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Registers the fake in place of {@link app.evaluation.llm.OpenAiClient} for tests.
 * {@code @Primary} rather than a profile exclusion, so the production wiring is exercised
 * as-is and only the port's implementation is swapped.
 */
@TestConfiguration
public class EvaluationTestConfig {

    @Bean
    @Primary
    public LlmClient llmClient() {
        return new FakeLlmClient();
    }
}
