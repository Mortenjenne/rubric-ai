# Provider-agnostic LLM port with retry; multi-provider failover deferred

The original goal was uptime even during an OpenAI outage, with Gemini as a fallback provider. We cut
the fallback from the first iteration to get a working, testable evaluation flow first, but kept the
seam it needs: an `LlmClient` port with a single `OpenAiClient` adapter behind it. Adding Gemini later
is a new adapter plus routing, not a redesign.

## Considered Options

Calling the OpenAI SDK directly and extracting an interface when a second provider arrives. Rejected
because the port costs almost nothing today and is the one piece of structure the stated resilience
goal depends on. What we deliberately did *not* build: failover orchestration, provider-selection
config, or a chain-of-responsibility with one link.

## Consequences

The failure taxonomy the port raises is shaped for a fallback that does not exist yet: 429, 5xx and
timeout are retried three times with exponential backoff; a schema-invalid response is re-asked once;
401 and 400 are surfaced immediately, because a malformed request would be rejected by any provider.
Today every exhausted path ends in `503` with an error code (`rate_limited`, `upstream_unavailable`,
`invalid_model_output`). When Gemini lands, those same branches become failover points instead of
terminal errors.
