package app.evaluation.domain;

/**
 * The provider could not be reached or failed to answer: a server error, a timeout, or a
 * refused connection. Server errors and timeouts are retried three times with exponential
 * backoff behind the {@link app.evaluation.llm.LlmClient} port; a refused connection is not
 * retried on the same path — a provider that refuses the connection outright is not going to
 * answer differently a moment later, so failing fast saves the backoff budget rather than
 * spending it against a provider that is plainly down.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
