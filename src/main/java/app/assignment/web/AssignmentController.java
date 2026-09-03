package app.assignment.web;

import app.assignment.service.AssignmentService;
import app.security.EducatorPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
