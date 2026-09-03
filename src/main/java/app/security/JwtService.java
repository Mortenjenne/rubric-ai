package app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies the JWTs that carry an Educator's identity across requests. Tokens are
 * long-lived with no refresh (per the spec): an Educator logs in again once a token expires
 * rather than the service tracking a refresh token.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(JwtProperties jwtProperties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = jwtProperties.expiration();
        this.clock = clock;
    }

    public String issue(UUID educatorId) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(educatorId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public UUID parseEducatorId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired bearer token", e);
        }
    }
}
