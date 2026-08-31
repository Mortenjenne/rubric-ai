# The suggested grade is emitted by the model, not computed from levels

Each Evaluation carries a suggested grade on the 7-trins-skala, produced by the LLM alongside the
per-criterion findings. The alternative was deriving it in code from the six Levels by a documented
rule (e.g. a Mangelfuldt on Formkrav caps the result). We chose the model's judgement because the
Levels are not commensurable — a weighted sum over four ordinal bands would imply an arithmetic
precision the pipeline does not have — and because the grade is explicitly advisory.

## Consequences

The grade is not reproducible in the way a computed one would be. `temperature: 0` reduces variance
but does not eliminate it, so every Evaluation records the provider, the model id and the rubric
version, making a disagreement between two runs explainable rather than mysterious.

The assignment brief requires the output be presented as *"en vejledende AI-baseret vurdering"* and
not as *"en automatisk sand bedømmelse."* A model-emitted grade sits closest to that line of anything
in this system. Three things keep it on the right side: Levels carry no grade values, the grade is
flagged `advisory` in the response, and the audience is the Educator, who is expected to overrule it.
Criterion Weights exist as guidance to the model about emphasis; nothing multiplies by them.
