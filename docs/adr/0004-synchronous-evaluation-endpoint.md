# The evaluation endpoint is synchronous

`POST /api/evaluations` blocks until the model responds, typically 20–60 seconds, with a 90-second
timeout. The alternative — 202 plus a job id and polling, or SSE streaming — would be the most
elaborate machinery in the codebase and would serve one educator evaluating one report at a time.

## Consequences

The endpoint holds a request thread for the length of the call, which is fine at this scale and would
not be under concurrent classroom use. The React frontend must show a progress state rather than a
spinner that looks hung. If evaluation ever runs over a whole class at once, this is the first
decision to revisit.
