package app.security.web;

import app.security.InvalidCredentialsException;
import app.security.LoginThrottledException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A wrong password and an unknown email both reach {@link InvalidCredentialsException} and so
 * both produce the exact same {@code 401} body — the endpoint must not be usable to discover
 * which emails have an account.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_credentials", "Invalid email or password"));
    }

    @ExceptionHandler(LoginThrottledException.class)
    public ResponseEntity<ErrorResponse> handleLoginThrottled(LoginThrottledException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("too_many_attempts", e.getMessage()));
    }
}
