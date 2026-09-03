package app.security;

/** A bearer token that failed to parse or verify: malformed, expired, or wrongly signed. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
