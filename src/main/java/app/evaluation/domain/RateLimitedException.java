package app.evaluation.domain;

/**
 * The provider rejected the request because too many calls were made too quickly (HTTP 429).
 * Retried three times with exponential backoff behind the
 * {@link app.evaluation.llm.LlmClient} port before this is thrown — by the time an Educator
 * sees it, the retry budget is already spent.
 */
public class RateLimitedException extends RuntimeException {

    public RateLimitedException(String message, Throwable cause) {
        super(message, cause);
    }
}
