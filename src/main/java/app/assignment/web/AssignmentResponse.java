package app.assignment.web;

import java.util.UUID;

public record AssignmentResponse(UUID id, String title, DraftResponse draft) {
}
