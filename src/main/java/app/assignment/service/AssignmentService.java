package app.assignment.service;

import app.assignment.Assignment;
import app.assignment.AssignmentNotFoundException;
import app.assignment.AssignmentRepository;
import app.assignment.AssignmentVersion;
import app.assignment.Criterion;
import app.assignment.DraftCriterionInput;
import app.assignment.web.AssignmentResponse;
import app.assignment.web.AssignmentSummaryResponse;
import app.assignment.web.AssignmentVersionSummaryResponse;
import app.assignment.web.CriterionResponse;
import app.assignment.web.DraftCriterionRequest;
import app.assignment.web.DraftResponse;
import app.assignment.web.ReplaceDraftRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Assignment authoring surface: creating one from a Template, listing and fetching an
 * Educator's own Assignments, replacing a Draft wholesale, and soft-deleting an Assignment. Every
 * lookup is scoped to the calling Educator, so another Educator's Assignment is a plain
 * {@link AssignmentNotFoundException} — a {@code 404}, never a {@code 403}.
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

    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> listAssignments(UUID educatorId) {
        return assignmentRepository.findAllByEducatorIdAndDeletedFalseOrderByUpdatedAtDesc(educatorId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID id, UUID educatorId) {
        return toResponse(findOwnedAssignment(id, educatorId));
    }

    @Transactional
    public AssignmentResponse replaceDraft(UUID id, UUID educatorId, ReplaceDraftRequest request) {
        Assignment assignment = findOwnedAssignment(id, educatorId);
        List<DraftCriterionInput> criteria = nullToEmpty(request.criteria()).stream().map(this::toInput).toList();
        assignment.replaceDraft(nullToEmpty(request.title()), nullToEmpty(request.assessmentStance()), criteria,
                Instant.now(clock));
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(UUID id, UUID educatorId) {
        Assignment assignment = findOwnedAssignment(id, educatorId);
        assignment.softDelete();
        assignmentRepository.save(assignment);
    }

    private Assignment findOwnedAssignment(UUID id, UUID educatorId) {
        return assignmentRepository.findByIdAndEducatorId(id, educatorId)
                .orElseThrow(() -> new AssignmentNotFoundException("No Assignment exists with id " + id));
    }

    private DraftCriterionInput toInput(DraftCriterionRequest request) {
        return new DraftCriterionInput(request.key(), nullToEmpty(request.name()), request.weight(),
                nullToEmpty(request.description()),
                request.sourceReferences() == null ? List.of() : request.sourceReferences(),
                request.levels() == null ? Map.of() : request.levels());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<DraftCriterionRequest> nullToEmpty(List<DraftCriterionRequest> value) {
        return value == null ? List.of() : value;
    }

    private AssignmentSummaryResponse toSummary(Assignment assignment) {
        Optional<AssignmentVersion> latestVersion = assignment.latestVersion();
        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getTitle(),
                latestVersion.isPresent(),
                latestVersion.map(AssignmentVersion::getVersionNumber).orElse(null),
                assignment.getUpdatedAt());
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        DraftResponse draft = new DraftResponse(
                assignment.getDraft().getAssessmentStance(),
                assignment.getDraft().getCriteria().stream().map(this::toResponse).toList());
        List<AssignmentVersionSummaryResponse> versions = assignment.getVersions().stream()
                .map(v -> new AssignmentVersionSummaryResponse(v.getVersionNumber(), v.getCreatedAt()))
                .toList();
        return new AssignmentResponse(assignment.getId(), assignment.getTitle(), draft, versions);
    }

    private CriterionResponse toResponse(Criterion criterion) {
        return new CriterionResponse(criterion.getKey(), criterion.getName(), criterion.getWeight(),
                criterion.getDescription(), criterion.getSourceReferences(), criterion.getLevels());
    }
}
