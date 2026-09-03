package app.assignment.web;

import java.util.List;
import java.util.Map;

public record CriterionResponse(String key, String name, int weight, String description,
                                 List<String> sourceReferences, Map<String, String> levels) {
}
