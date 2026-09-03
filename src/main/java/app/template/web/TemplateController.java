package app.template.web;

import app.template.Template;
import app.template.TemplateCatalog;
import app.template.TemplateCriterion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateCatalog templateCatalog;

    public TemplateController(TemplateCatalog templateCatalog) {
        this.templateCatalog = templateCatalog;
    }

    @GetMapping
    public List<TemplateSummaryResponse> listTemplates() {
        return templateCatalog.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    private TemplateSummaryResponse toSummary(Template template) {
        List<String> criteriaNames = template.criteria().stream().map(TemplateCriterion::name).toList();
        return new TemplateSummaryResponse(template.id(), template.title(), template.description(), criteriaNames);
    }
}
