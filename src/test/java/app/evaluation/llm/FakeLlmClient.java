package app.evaluation.llm;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The one substitution seam the whole evaluation flow tests use: a queue of canned raw
 * payloads, returned one per call in order. Supports the re-ask scenario (ticket 04) by
 * queuing more than one response, and the provider-failure scenarios (ticket 05) by queuing an
 * exception in place of a payload — standing in for a port that has already exhausted its own
 * retry or fail-fast handling before the failure reaches the evaluation service.
 */
public class FakeLlmClient implements LlmClient {

    private final Deque<Object> responses = new ArrayDeque<>();
    private int callCount = 0;

    public void enqueue(String rawResponse) {
        responses.addLast(rawResponse);
    }

    public void enqueueFailure(RuntimeException failure) {
        responses.addLast(failure);
    }

    public int callCount() {
        return callCount;
    }

    /** Clears any queued responses and resets the call count — the bean is a Spring singleton
     * shared across every test in the class, so each test starts from a clean slate. */
    public void reset() {
        responses.clear();
        callCount = 0;
    }

    @Override
    public String call(LlmRequest request) {
        callCount++;
        if (responses.isEmpty()) {
            throw new IllegalStateException("FakeLlmClient has no canned response queued");
        }
        Object next = responses.removeFirst();
        if (next instanceof RuntimeException failure) {
            throw failure;
        }
        return (String) next;
    }
}
