package app.rubric.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CriterionResource(
        String id,
        String name,
        int weight,
        @JsonProperty("sources") List<String> sourceMaterial,
        String description,
        Map<String, String> levels) {
}
