package app.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per email so an unverified password is not brute-forceable.
 * State is in-memory: a restart clears it, which is acceptable for a login attempt counter and
 * avoids a schema for something that must never outlive the account it is throttling.
 */
@Component
public class LoginThrottle {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration COOLDOWN = Duration.ofMinutes(5);

    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    /**
     * @throws LoginThrottledException if this email has failed {@value #MAX_ATTEMPTS} times
     *                                  within the last cooling-off period.
     */
    public void checkNotBlocked(String email) {
        String key = normalise(email);
        Attempts attempts = attemptsByEmail.get(key);
        if (attempts == null || attempts.count() < MAX_ATTEMPTS) {
            return;
        }
        if (Instant.now(clock).isBefore(attempts.blockedUntil())) {
            throw new LoginThrottledException("Too many failed login attempts; try again later");
        }
        // Cooldown elapsed: let this attempt through and start counting fresh from it.
        attemptsByEmail.remove(key);
    }

    public void recordFailure(String email) {
        String key = normalise(email);
        Instant blockedUntil = Instant.now(clock).plus(COOLDOWN);
        attemptsByEmail.compute(key, (k, existing) ->
                new Attempts(existing == null ? 1 : existing.count() + 1, blockedUntil));
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalise(email));
    }

    private String normalise(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private record Attempts(int count, Instant blockedUntil) {
    }
}
