package app.security;

/** A wrong password or an unknown email — deliberately indistinguishable to the caller. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
