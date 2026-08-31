package app.evaluation.llm;

/**
 * The single seam between the evaluation service and a language model provider.
 * Exactly one production implementation exists ({@link OpenAiClient}); tests substitute
 * a fake here and let every other component — prompt assembly, parsing, validation,
 * persistence — run for real. Retry and failover live behind this port, not in front of it.
 */
public interface LlmClient {

    /**
     * @return the raw response payload, exactly as the provider returned it — the caller
     * is responsible for parsing and validating it before trusting anything in it.
     */
    String call(LlmRequest request);
}
