package app.template.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Shape of one Criterion inside a bundled Template JSON resource under {@code classpath:templates/}. */
public record CriterionResource(
        String id,
        String name,
        int weight,
        @JsonProperty("sources") List<String> sourceReferences,
        String description,
        Map<String, String> levels) {
}
