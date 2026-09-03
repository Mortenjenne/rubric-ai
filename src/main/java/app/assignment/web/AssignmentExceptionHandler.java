package app.assignment.web;

import app.assignment.AssignmentNotFoundException;
import app.template.TemplateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps Assignment failures to HTTP responses. An unknown Template id or an Assignment id that
 * doesn't resolve for the calling Educator are both plain 404s — the id is simply wrong (or
 * belongs to someone else), not something a retry could fix.
 */
@RestControllerAdvice
public class AssignmentExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("template_not_found", e.getMessage()));
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentNotFound(AssignmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("assignment_not_found", e.getMessage()));
    }
}
