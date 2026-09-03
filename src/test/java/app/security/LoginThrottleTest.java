package app.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginThrottleTest {

    /** LoginThrottle's own constants: 5 failures trip it, a 5-minute cooldown lifts it. */
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration COOLDOWN = Duration.ofMinutes(5);

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final Clock movableClock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    private final LoginThrottle loginThrottle = new LoginThrottle(movableClock);

    @Test
    void fewerThanTheMaxFailuresDoesNotBlock() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            loginThrottle.recordFailure("someone@example.com");
        }

        assertThatCode(() -> loginThrottle.checkNotBlocked("someone@example.com")).doesNotThrowAnyException();
    }

    @Test
    void theMaxNumberOfFailuresBlocksFurtherAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginThrottle.recordFailure("someone@example.com");
        }

        assertThatThrownBy(() -> loginThrottle.checkNotBlocked("someone@example.com"))
                .isInstanceOf(LoginThrottledException.class);
    }

    @Test
    void blockingIsCaseInsensitiveOnEmail() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginThrottle.recordFailure("Someone@Example.com");
        }

        assertThatThrownBy(() -> loginThrottle.checkNotBlocked("someone@example.com"))
                .isInstanceOf(LoginThrottledException.class);
    }

    @Test
    void aSuccessClearsPriorFailures() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            loginThrottle.recordFailure("someone@example.com");
        }
        loginThrottle.recordSuccess("someone@example.com");
        loginThrottle.recordFailure("someone@example.com");

        assertThatCode(() -> loginThrottle.checkNotBlocked("someone@example.com")).doesNotThrowAnyException();
    }

    @Test
    void theBlockLiftsOnceTheCooldownElapses() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginThrottle.recordFailure("someone@example.com");
        }
        now.updateAndGet(instant -> instant.plus(COOLDOWN).plusSeconds(1));

        assertThatCode(() -> loginThrottle.checkNotBlocked("someone@example.com")).doesNotThrowAnyException();
    }
}
