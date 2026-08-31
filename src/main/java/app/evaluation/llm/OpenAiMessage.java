package app.evaluation.llm;

/**
 * One message in a Chat Completions request, as OpenAI expects it on the wire.
 */
record OpenAiMessage(String role, String content) {
}
