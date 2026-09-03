package app.template;

import app.template.json.CriterionResource;
import app.template.json.TemplateResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The classpath-bundled starting points an Educator can copy into their own Assignment. Read
 * from {@code classpath:templates/*.json} — never a database table, never seeded, never owned by
 * an Educator, so fixing a typo in a Level descriptor is a resource edit, not a migration.
 */
@Component
public class TemplateCatalog {

    private static final String LOCATION_PATTERN = "classpath:templates/*.json";

    private final Map<String, Template> templatesById;

    public TemplateCatalog(ObjectMapper objectMapper) {
        this.templatesById = loadAll(objectMapper);
    }

    public List<Template> findAll() {
        return templatesById.values().stream()
                .sorted(Comparator.comparing(Template::id))
                .toList();
    }

    public Optional<Template> findById(String id) {
        return Optional.ofNullable(templatesById.get(id));
    }

    private Map<String, Template> loadAll(ObjectMapper objectMapper) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION_PATTERN);
            Map<String, Template> byId = new LinkedHashMap<>();
            for (Resource resource : resources) {
                Template template = readTemplate(objectMapper, resource);
                byId.put(template.id(), template);
            }
            return Map.copyOf(byId);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load bundled Assignment templates", e);
        }
    }

    private Template readTemplate(ObjectMapper objectMapper, Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            TemplateResource parsed = objectMapper.readValue(in, TemplateResource.class);
            List<TemplateCriterion> criteria = parsed.criteria().stream()
                    .map(this::toCriterion)
                    .toList();
            return new Template(parsed.id(), parsed.title(), parsed.description(),
                    parsed.assessmentStance(), criteria);
        }
    }

    private TemplateCriterion toCriterion(CriterionResource resource) {
        return new TemplateCriterion(resource.id(), resource.name(), resource.weight(),
                resource.description(), resource.sourceReferences(), resource.levels());
    }
}
