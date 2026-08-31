package app.evaluation.web;

import app.evaluation.EvaluationService;
import app.evaluation.json.EvaluationRequest;
import app.evaluation.json.EvaluationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
        return evaluationService.evaluate(request);
    }
}
