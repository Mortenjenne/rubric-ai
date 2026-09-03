package app.template.json;

import java.util.List;

/** Shape of a bundled Template JSON resource under {@code classpath:templates/}. */
public record TemplateResource(
        String id,
        String title,
        String description,
        String assessmentStance,
        List<CriterionResource> criteria) {
}
