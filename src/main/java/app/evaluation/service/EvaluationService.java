package app.evaluation.service;

import app.evaluation.domain.InvalidModelOutputException;
import app.evaluation.llm.LlmClient;
import app.evaluation.llm.LlmEvaluationPayload;
import app.evaluation.llm.LlmFindingPayload;
import app.evaluation.llm.LlmProperties;
import app.evaluation.llm.LlmRequest;
import app.evaluation.llm.PromptBuilder;
import app.evaluation.persistence.Evaluation;
import app.evaluation.persistence.EvaluationDocument;
import app.evaluation.persistence.EvaluationRepository;
import app.evaluation.persistence.FindingDocument;
import app.evaluation.web.EvaluationRequest;
import app.evaluation.web.EvaluationResponse;
import app.evaluation.web.FindingResponse;
import app.evaluation.web.SuggestedGradeResponse;
import app.rubric.Criterion;
import app.rubric.Rubric;
import app.rubric.RubricRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates one evaluation end to end: load the active Rubric, build the prompts, call the
 * language model port, parse and validate the response, persist it and return it. The only
 * seam is {@link LlmClient} — everything else here runs for real in tests.
 */
@Service
public class EvaluationService {

    private final RubricRepository rubricRepository;
    private final EvaluationRepository evaluationRepository;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LlmProperties llmProperties;
    private final Clock clock;

    public EvaluationService(RubricRepository rubricRepository,
                              EvaluationRepository evaluationRepository,
                              LlmClient llmClient,
                              PromptBuilder promptBuilder,
                              ObjectMapper objectMapper,
                              Validator validator,
                              LlmProperties llmProperties,
                              Clock clock) {
        this.rubricRepository = rubricRepository;
        this.evaluationRepository = evaluationRepository;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.llmProperties = llmProperties;
        this.clock = clock;
    }

    @Transactional
    public EvaluationResponse evaluate(EvaluationRequest request) {
        Rubric rubric = loadActiveRubric();
        LlmRequest llmRequest = promptBuilder.build(rubric, request.submissionText());
        String raw = llmClient.call(llmRequest);
        LlmEvaluationPayload payload = parseAndValidate(raw);
        Map<String, LlmFindingPayload> findingsByCriterion = indexByCriterion(rubric, payload);

        List<FindingDocument> findingDocuments = new ArrayList<>();
        for (Criterion criterion : rubric.getCriteria()) {
            LlmFindingPayload finding = findingsByCriterion.get(criterion.getKey());
            findingDocuments.add(new FindingDocument(
                    criterion.getKey(),
                    criterion.getName(),
                    criterion.getWeight(),
                    finding.level().label(),
                    finding.strengths(),
                    finding.weaknesses(),
                    finding.improvements(),
                    finding.evidence()));
        }

        Evaluation evaluation = new Evaluation(
                UUID.randomUUID(),
                rubric.getVersion(),
                llmProperties.provider(),
                llmProperties.model(),
                payload.suggestedGrade().label(),
                Instant.now(clock),
                new EvaluationDocument(payload.overallAssessment(), findingDocuments, payload.dialogueQuestions()));

        evaluationRepository.save(evaluation);

        return toResponse(evaluation);
    }

    private Rubric loadActiveRubric() {
        return rubricRepository.findFirstByOrderByVersionDesc()
                .orElseThrow(() -> new IllegalStateException(
                        "No Rubric is seeded; the service cannot evaluate a Submission without one"));
    }

    private LlmEvaluationPayload parseAndValidate(String raw) {
        LlmEvaluationPayload payload;
        try {
            payload = objectMapper.readValue(raw, LlmEvaluationPayload.class);
        } catch (JsonProcessingException e) {
            throw new InvalidModelOutputException("Model response is not valid JSON", e);
        }

        Set<ConstraintViolation<LlmEvaluationPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new InvalidModelOutputException("Model response failed validation: " + detail);
        }

        return payload;
    }

    /**
     * A Finding must reference exactly one Criterion from the active Rubric — no fewer, no
     * more. Verifying quotes, enforcing Rubric order and the single re-ask belong to a later
     * ticket; this is the minimum needed to build a response at all without a Finding for an
     * unknown Criterion silently vanishing.
     */
    private Map<String, LlmFindingPayload> indexByCriterion(Rubric rubric, LlmEvaluationPayload payload) {
        Set<String> knownCriteria = rubric.getCriteria().stream().map(Criterion::getKey).collect(Collectors.toSet());

        Map<String, LlmFindingPayload> byCriterion = new HashMap<>();
        for (LlmFindingPayload finding : payload.findings()) {
            if (!knownCriteria.contains(finding.criterion())) {
                throw new InvalidModelOutputException(
                        "Model response references unknown criterion '" + finding.criterion() + "'");
            }
            byCriterion.put(finding.criterion(), finding);
        }

        if (!byCriterion.keySet().equals(knownCriteria)) {
            throw new InvalidModelOutputException(
                    "Model response is missing a Finding for one or more Rubric criteria");
        }

        return byCriterion;
    }

    private EvaluationResponse toResponse(Evaluation evaluation) {
        EvaluationDocument document = evaluation.getDocument();
        List<FindingResponse> findings = document.findings().stream()
                .map(f -> new FindingResponse(
                        f.criterion(), f.criterionName(), f.weight(), f.level(),
                        f.strengths(), f.weaknesses(), f.improvements(), f.evidence()))
                .toList();

        return new EvaluationResponse(
                evaluation.getId().toString(),
                evaluation.getRubricVersion(),
                evaluation.getProvider(),
                evaluation.getModel(),
                evaluation.getCreatedAt(),
                document.overallAssessment(),
                new SuggestedGradeResponse(evaluation.getSuggestedGrade()),
                findings,
                document.dialogueQuestions());
    }
}
