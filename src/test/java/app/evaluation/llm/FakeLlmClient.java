package app.evaluation.llm;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The one substitution seam the whole evaluation flow tests use: a queue of canned raw
 * payloads, returned one per call in order. Supports the re-ask scenario (ticket 04) by
 * queuing more than one response.
 */
public class FakeLlmClient implements LlmClient {

    private final Deque<String> responses = new ArrayDeque<>();
    private int callCount = 0;

    public void enqueue(String rawResponse) {
        responses.addLast(rawResponse);
    }

    public int callCount() {
        return callCount;
    }

    @Override
    public String call(LlmRequest request) {
        callCount++;
        if (responses.isEmpty()) {
            throw new IllegalStateException("FakeLlmClient has no canned response queued");
        }
        return responses.removeFirst();
    }
}
