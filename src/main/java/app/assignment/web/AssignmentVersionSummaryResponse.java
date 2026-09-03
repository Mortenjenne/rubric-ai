package app.assignment.web;

import java.time.Instant;

public record AssignmentVersionSummaryResponse(int versionNumber, Instant createdAt) {
}
