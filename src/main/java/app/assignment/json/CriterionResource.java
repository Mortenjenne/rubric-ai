package app.assignment.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CriterionResource(
        String id,
        String name,
        int weight,
        @JsonProperty("sources") List<String> sourceReferences,
        String description,
        Map<String, String> levels) {
}
