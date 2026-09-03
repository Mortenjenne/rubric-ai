# The Educator edits the assessment stance, never the output contract

The model's instructions are assembled from two parts. The Assessment stance — the level to expect,
what counts as evidence of a Criterion, what a weakness may be about — is free prose owned by the
Educator and appended to the system prompt. Everything else is owned by code and cannot be edited:
the advisory framing, the rule that a Level is chosen from the Rubric's own descriptors, the
requirement that evidence be copied verbatim, the JSON schema, and Danish output.

The split is not a matter of taste. `EvaluationService` deserialises the response, Bean-Validates it,
requires exactly one Finding per Criterion, and rejects any evidence quote that is not a literal
substring of the Submission. An Educator who could edit those instructions could make every one of
those checks fail, on every request, with no way to tell them why. The advisory framing is fixed for
a different reason: that the output is not a final mark is the product's position (ADR 0002), not a
teacher's preference, and no Educator should be able to delete it and then hand the result to a
censor.

## Considered options

Letting the Educator own the whole prompt was rejected for the reasons above. A validation pass — a
test run against a sample Submission before a stance can be saved — remains a reasonable later
addition, and would not change this split.

## Consequences

`PromptBuilder` no longer holds any assignment-specific text; the calibration and anti-keyword rules
that used to live in it move into the praktikrapport template's stance. The stance is appended to the
system prompt rather than placed in the user prompt, so that instructions about how to judge are
never in the same message as the Submission being judged.
