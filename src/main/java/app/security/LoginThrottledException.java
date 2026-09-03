package app.security;

/** Too many failed login attempts for one email within the cooling-off period. */
public class LoginThrottledException extends RuntimeException {

    public LoginThrottledException(String message) {
        super(message);
    }
}
