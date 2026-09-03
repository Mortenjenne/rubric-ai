package app.assignment.web;

import java.time.Instant;
import java.util.List;

public record AssignmentVersionResponse(int versionNumber, String title, String assessmentStance,
                                         List<CriterionResponse> criteria, Instant createdAt) {
}
