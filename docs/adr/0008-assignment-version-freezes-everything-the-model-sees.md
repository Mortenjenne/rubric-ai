# An Assignment version freezes everything the model sees

Publishing an Assignment snapshots its Rubric, its Assessment stance and its Source material together
into one immutable, per-Assignment version number. An Evaluation records that number, and two
Evaluations carrying the same one are guaranteed to have been produced from identical inputs.

Versioning the Rubric alone was the obvious smaller option and is the one to avoid: two Evaluations
could then share a version number having been judged with a different stance or a different set of
documents, which makes the recorded version actively misleading rather than merely incomplete. The
whole point of recording provenance is to answer "are these two comparable?", and only a version
covering every input can answer it.

## Consequences

The version is no longer a Rubric version, and no longer global — it is scoped to its Assignment and
restarts at 1. `Evaluation` therefore records an Assignment and a version rather than the old global
`rubric_version`, and the version is nullable: an Evaluation run against the Draft stores null and is
marked as not comparable. Evaluating against the Draft is allowed deliberately, so that tuning a
stance does not require publishing an immutable version per wording change.

Source material is copied into each published version rather than referenced, which duplicates
document text across versions. At the 50.000-character cap (ADR 0007) that is a few hundred kilobytes
for a heavily revised Assignment — cheaper than content-addressed storage is to build and reason
about.
