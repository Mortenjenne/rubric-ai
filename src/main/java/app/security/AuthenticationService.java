package app.security;

import app.educator.Educator;
import app.educator.EducatorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifies an Educator's email and password and issues a JWT. A wrong password and an unknown
 * email are deliberately indistinguishable: both throw {@link InvalidCredentialsException} with
 * the same message, and an unknown email still runs a bcrypt comparison against a dummy hash —
 * skipping it would make "no such account" answer measurably faster than "wrong password" and
 * defeat the point of returning the same response.
 */
@Service
public class AuthenticationService {

    private final EducatorRepository educatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;
    private final String dummyPasswordHash;

    public AuthenticationService(EducatorRepository educatorRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  LoginThrottle loginThrottle) {
        this.educatorRepository = educatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottle = loginThrottle;
        this.dummyPasswordHash = passwordEncoder.encode("timing-camouflage-" + UUID.randomUUID());
    }

    public String login(String email, String password) {
        loginThrottle.checkNotBlocked(email);

        Optional<Educator> educator = educatorRepository.findByEmail(email);
        String hashToCompareAgainst = educator.map(Educator::getPasswordHash).orElse(dummyPasswordHash);
        boolean valid = educator.isPresent() && passwordEncoder.matches(password, hashToCompareAgainst);

        if (!valid) {
            loginThrottle.recordFailure(email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        loginThrottle.recordSuccess(email);
        return jwtService.issue(educator.get().getId());
    }
}
