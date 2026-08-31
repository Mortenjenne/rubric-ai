# 05: Fail honestly when the provider does

**What to build:** Predictable behaviour when the model call goes wrong. The Educator should never
receive half an Evaluation, and should be told which kind of failure occurred — whether it is worth
retrying now or coming back later.

Different failures deserve different treatment. Rate limits, server errors and timeouts are worth
retrying: three attempts with exponential backoff. A refused connection is not worth retrying on the
same path — fail fast rather than burning the backoff budget on a provider that is plainly down.
Authentication and malformed-request faults are never retried: they mean our request is wrong, and
repeating it changes nothing.

When every path is exhausted the request ends in an error naming its cause — rate limited, upstream
unavailable, or invalid model output — and no Evaluation is persisted. Configuration faults surface
separately from that family, because they are our bug rather than the provider's outage.

This taxonomy is deliberately shaped for a fallback provider that does not exist yet (see the ADR on
the provider port). Each exhausted branch is where a second provider would later attach, so keep them
distinct even though today they all end the request.

**Blocked by:** 03, 04

**Status:** ready-for-agent

- [x] Rate limits, server errors and timeouts are retried three times with exponential backoff
- [x] A refused connection fails fast without exhausting the retry budget
- [x] Authentication and malformed-request faults are never retried
- [x] Exhausted retries return a service-unavailable response carrying a machine-readable cause
- [x] Output that fails validation after its single re-ask returns the invalid-model-output cause
- [x] Configuration faults surface as a server error distinct from the service-unavailable family
- [x] No Evaluation is persisted on any failure path
- [x] Retry and failover branches live behind the port, not in the evaluation service
- [x] Every failure path is covered by a test driving the endpoint with a fake port that simulates that failure

## Comments

Implemented in `c4f7f7b`: `LlmConfigurationException`, `RateLimitedException` and
`UpstreamUnavailableException` added behind the `LlmClient` port; `OpenAiClient` retries
429/5xx/timeout three times with exponential backoff, fails fast on a refused connection, and
never retries 401/400; `EvaluationExceptionHandler` maps the three causes to `503`
(`rate_limited`, `upstream_unavailable`) and `500` (`configuration_error`). Every branch is
covered by a fake-port-driven endpoint test plus adapter-level tests against a mocked HTTP
server.

Code review (`/code-review 8631be1`) found no hard Standards or Spec violations. The one real
finding — other 4xx codes (403, 404, 422, …) fell through uncaught instead of being classified —
was fixed in `91957f5`: any other 4xx now also surfaces as `LlmConfigurationException`, and ADR
0001 was updated to match. Pushed to `origin/main`.
