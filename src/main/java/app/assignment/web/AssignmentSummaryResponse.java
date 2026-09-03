package app.assignment.web;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSummaryResponse(UUID id, String title, boolean hasPublishedVersion,
                                         Integer latestVersionNumber, Instant lastEditedAt) {
}
