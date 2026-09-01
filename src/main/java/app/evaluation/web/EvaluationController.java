package app.evaluation.web;

import app.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    @GetMapping
    public List<EvaluationSummaryResponse> listEvaluations() {
        return evaluationService.listEvaluations();
    }

    @GetMapping("/{id}")
    public EvaluationResponse getEvaluation(@PathVariable UUID id) {
        return evaluationService.getEvaluation(id);
    }
}
