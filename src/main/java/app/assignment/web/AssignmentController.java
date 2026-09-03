package app.assignment.web;

import app.assignment.service.AssignmentService;
import app.security.EducatorPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public AssignmentResponse createFromTemplate(@Valid @RequestBody CreateAssignmentRequest request,
                                                  @AuthenticationPrincipal EducatorPrincipal principal) {
        return assignmentService.createFromTemplate(principal.educatorId(), request.templateId());
    }

    @GetMapping
    public List<AssignmentSummaryResponse> listAssignments(@AuthenticationPrincipal EducatorPrincipal principal) {
        return assignmentService.listAssignments(principal.educatorId());
    }

    @GetMapping("/{id}")
    public AssignmentResponse getAssignment(@PathVariable UUID id,
                                             @AuthenticationPrincipal EducatorPrincipal principal) {
        return assignmentService.getAssignment(id, principal.educatorId());
    }

    @PutMapping("/{id}/draft")
    public AssignmentResponse replaceDraft(@PathVariable UUID id,
                                            @RequestBody ReplaceDraftRequest request,
                                            @AuthenticationPrincipal EducatorPrincipal principal) {
        return assignmentService.replaceDraft(id, principal.educatorId(), request);
    }

    @PostMapping("/{id}/versions")
    public AssignmentVersionResponse publish(@PathVariable UUID id,
                                              @AuthenticationPrincipal EducatorPrincipal principal) {
        return assignmentService.publish(id, principal.educatorId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable UUID id, @AuthenticationPrincipal EducatorPrincipal principal) {
        assignmentService.deleteAssignment(id, principal.educatorId());
    }
}
