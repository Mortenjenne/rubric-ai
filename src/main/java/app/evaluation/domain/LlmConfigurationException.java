package app.evaluation.domain;

/**
 * The request to the provider was wrong in a way that retrying does not fix: no credential
 * configured, credentials the provider rejects, or a request the provider rejects as malformed.
 * This is our bug, not the provider's outage — kept distinct from
 * {@link UpstreamUnavailableException} and {@link RateLimitedException} so it surfaces as a
 * server error rather than joining the service-unavailable family those two share.
 */
public class LlmConfigurationException extends RuntimeException {

    public LlmConfigurationException(String message) {
        super(message);
    }

    public LlmConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
