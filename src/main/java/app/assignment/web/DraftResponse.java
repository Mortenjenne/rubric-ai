package app.assignment.web;

import java.util.List;

public record DraftResponse(String assessmentStance, List<CriterionResponse> criteria) {
}
