package app.assignment.web;

import app.template.TemplateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps Assignment-creation failures to HTTP responses. An unknown Template id is a plain 404 —
 * the id is simply wrong, not something a retry could fix.
 */
@RestControllerAdvice
public class AssignmentExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("template_not_found", e.getMessage()));
    }
}
