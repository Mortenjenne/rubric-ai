package app.assignment.service;

import app.assignment.Assignment;
import app.assignment.AssignmentRepository;
import app.assignment.Criterion;
import app.assignment.web.AssignmentResponse;
import app.assignment.web.CriterionResponse;
import app.assignment.web.DraftResponse;
import app.educator.Educator;
import app.educator.EducatorRepository;
import app.template.Template;
import app.template.TemplateCatalog;
import app.template.TemplateCriterion;
import app.template.TemplateNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Creates an Assignment by copying a bundled Template: its Rubric and Assessment stance land in
 * a fresh Draft owned by the calling Educator, with no published versions. The copy is
 * independent of the Template from the moment it is made — {@link Assignment#addDraftCriterion}
 * builds brand new Criterion rows, so nothing here is shared with the Template or with any other
 * Assignment copied from it.
 */
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final EducatorRepository educatorRepository;
    private final TemplateCatalog templateCatalog;
    private final Clock clock;

    public AssignmentService(AssignmentRepository assignmentRepository,
                              EducatorRepository educatorRepository,
                              TemplateCatalog templateCatalog,
                              Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.educatorRepository = educatorRepository;
        this.templateCatalog = templateCatalog;
        this.clock = clock;
    }

    @Transactional
    public AssignmentResponse createFromTemplate(UUID educatorId, String templateId) {
        Template template = templateCatalog.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("No template exists with id " + templateId));
        Educator educator = educatorRepository.findById(educatorId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated educator " + educatorId + " does not exist"));

        Assignment assignment = new Assignment(UUID.randomUUID(), educator, template.title(), Instant.now(clock));
        assignment.setDraftAssessmentStance(template.assessmentStance());
        for (TemplateCriterion criterion : template.criteria()) {
            assignment.addDraftCriterion(criterion.key(), criterion.name(), criterion.weight(),
                    criterion.description(), criterion.sourceReferences(), criterion.levels());
        }
        return toResponse(assignmentRepository.save(assignment));
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        DraftResponse draft = new DraftResponse(
                assignment.getDraft().getAssessmentStance(),
                assignment.getDraft().getCriteria().stream().map(this::toResponse).toList());
        return new AssignmentResponse(assignment.getId(), assignment.getTitle(), draft);
    }

    private CriterionResponse toResponse(Criterion criterion) {
        return new CriterionResponse(criterion.getKey(), criterion.getName(), criterion.getWeight(),
                criterion.getDescription(), criterion.getSourceReferences(), criterion.getLevels());
    }
}
