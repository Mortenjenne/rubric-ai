package app.evaluation.llm;

import app.rubric.Criterion;
import app.rubric.Level;
import app.rubric.Rubric;
import org.springframework.stereotype.Component;

/**
 * Assembles the two prompts from the active Rubric and a Submission's text. The system
 * prompt is in English and fixed; the user prompt is English scaffolding around the
 * Rubric's Danish content and the Submission text.
 */
@Component
public class PromptBuilder {

    public LlmRequest build(Rubric rubric, String submissionText) {
        return new LlmRequest(systemPrompt(), userPrompt(rubric, submissionText));
    }

    private String systemPrompt() {
        StringBuilder levelNames = new StringBuilder();
        for (Level level : Level.values()) {
            if (!levelNames.isEmpty()) {
                levelNames.append(", ");
            }
            levelNames.append('"').append(level.label()).append('"');
        }

        return """
                You are an assistant helping an Educator on a Danish AP degree programme assess a \
                student's practicum report (a "Submission") against a fixed assessment Rubric. You \
                produce a structured, advisory evaluation only. Never present your output as a final \
                grade or an authoritative decision — the Educator makes that judgement; your output is \
                a starting point for their own reading, not a replacement for it.

                Respond with raw JSON only: a single JSON object, with no markdown code fences, no \
                text before or after it, and no comments inside it.

                The JSON object must have exactly these top-level fields:
                - "overallAssessment": a prose paragraph
                - "suggestedGrade": one of the exact strings "-3", "00", "02", "4", "7", "10", "12" \
                (the 7-trins-skala)
                - "findings": an array with exactly one entry per Criterion listed in the user \
                message, in the same order they are listed there
                - "dialogueQuestions": an array of four to six strings

                Each entry in "findings" must have exactly these fields:
                - "criterion": the Criterion's id, copied exactly as given in the user message
                - "level": exactly one of %s
                - "strengths": an array of strings
                - "weaknesses": an array of strings
                - "improvements": an array of strings
                - "evidence": an array of strings, each one a verbatim excerpt copied exactly from \
                the submission text, character for character — never paraphrase or reconstruct a quote

                All text you write for the Educator to read — overallAssessment, and every string \
                inside strengths, weaknesses, improvements, evidence and dialogueQuestions — must be \
                written in Danish. These instructions to you are in English; that does not change the \
                required output language.
                """.formatted(levelNames);
    }

    private String userPrompt(Rubric rubric, String submissionText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Rubric for ").append(rubric.getAssignment()).append('\n');
        if (rubric.getNote() != null && !rubric.getNote().isBlank()) {
            prompt.append(rubric.getNote()).append('\n');
        }
        prompt.append('\n');

        for (Criterion criterion : rubric.getCriteria()) {
            prompt.append("Criterion id: ").append(criterion.getKey()).append('\n');
            prompt.append("Name: ").append(criterion.getName()).append('\n');
            prompt.append("Weight: ").append(criterion.getWeight()).append('\n');
            prompt.append("Description: ").append(criterion.getDescription()).append('\n');
            for (Level level : Level.values()) {
                String descriptor = criterion.getLevels().get(level.label());
                if (descriptor != null) {
                    prompt.append(level.label()).append(": ").append(descriptor).append('\n');
                }
            }
            prompt.append('\n');
        }

        prompt.append("Submission text:\n").append(submissionText);
        return prompt.toString();
    }
}
