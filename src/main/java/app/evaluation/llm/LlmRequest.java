package app.evaluation.llm;

/**
 * A model call as the evaluation service assembles it: the English system prompt
 * establishing the model's role and output rules, and the user prompt carrying the
 * Rubric and the Submission text.
 */
public record LlmRequest(String systemPrompt, String userPrompt) {
}
