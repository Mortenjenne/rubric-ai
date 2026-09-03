package app.assignment.web;

import jakarta.validation.constraints.NotBlank;

public record CreateAssignmentRequest(@NotBlank String templateId) {
}
