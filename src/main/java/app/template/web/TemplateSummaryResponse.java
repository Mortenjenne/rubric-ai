package app.template.web;

import java.util.List;

public record TemplateSummaryResponse(String id, String title, String description, List<String> criteriaNames) {
}
