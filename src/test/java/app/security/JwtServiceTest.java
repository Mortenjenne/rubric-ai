package app.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "at-least-32-bytes-long-signing-secret-for-tests";

    // JJWT's own expiry check runs against the real wall clock regardless of what Clock a
    // JwtService is built with, so the clock here is real time rather than a fixed instant —
    // a fixed instant far from "now" would make every issued token look already expired.
    private JwtService jwtService(String secret, Duration expiration) {
        return new JwtService(new JwtProperties(secret, expiration), Clock.systemUTC());
    }

    @Test
    void aTokenParsesBackToTheEducatorIdItWasIssuedFor() {
        JwtService jwtService = jwtService(SECRET, Duration.ofDays(30));
        UUID educatorId = UUID.randomUUID();

        String token = jwtService.issue(educatorId);

        assertThat(jwtService.parseEducatorId(token)).isEqualTo(educatorId);
    }

    @Test
    void anExpiredTokenIsRejected() {
        JwtService jwtService = jwtService(SECRET, Duration.ofSeconds(-1));

        String token = jwtService.issue(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseEducatorId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void aTokenSignedWithADifferentSecretIsRejected() {
        JwtService issuer = jwtService(SECRET, Duration.ofDays(30));
        JwtService verifier = jwtService("a-completely-different-32-byte-plus-secret-value", Duration.ofDays(30));

        String token = issuer.issue(UUID.randomUUID());

        assertThatThrownBy(() -> verifier.parseEducatorId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void garbageIsRejectedRatherThanThrowingAnUncheckedJwtException() {
        JwtService jwtService = jwtService(SECRET, Duration.ofDays(30));

        assertThatThrownBy(() -> jwtService.parseEducatorId("not-a-jwt-at-all"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
