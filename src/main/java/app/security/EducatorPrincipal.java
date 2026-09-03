package app.security;

import java.util.UUID;

/** The authenticated principal a valid bearer token resolves to. */
public record EducatorPrincipal(UUID educatorId) {
}
