package app.assignment.web;

import java.util.List;

public record ValidationErrorResponse(String code, List<String> errors) {
}
