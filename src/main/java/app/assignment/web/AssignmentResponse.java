package app.assignment.web;

import java.util.List;
import java.util.UUID;

public record AssignmentResponse(UUID id, String title, DraftResponse draft,
                                  List<AssignmentVersionSummaryResponse> versions) {
}
