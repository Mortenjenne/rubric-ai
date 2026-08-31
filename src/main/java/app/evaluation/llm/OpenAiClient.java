package app.evaluation.llm;

import org.springframework.stereotype.Component;

/**
 * The production {@link LlmClient}. The call to OpenAI itself — native structured-output
 * enforcement, temperature, timeout, retry and the configured model id — lands in a later
 * ticket; this class exists now so the port has exactly one production implementation from
 * the start, and only that implementation is missing its real behaviour.
 */
@Component
public class OpenAiClient implements LlmClient {

    @Override
    public String call(LlmRequest request) {
        throw new UnsupportedOperationException(
                "OpenAI integration is not implemented yet; see issue 03-call-openai-for-real");
    }
}
