# Source material is sent to the model

An Assignment's Source material — the course documents it is defined by — is uploaded by the
Educator, stored, and concatenated into the user prompt above the Rubric on every evaluation. This
reverses the position recorded when the Rubric was hand-built from those documents by a human and
only the Rubric was ever shown to the model.

The reversal is what makes the service usable outside the one Assignment it was written for. A
Rubric authored by an Educator in an afternoon carries far less of an Assignment's meaning than one
distilled by hand over weeks, and the documents are the cheapest way to give the model back what the
Rubric leaves out. Source material is the Educator's own course documents, not a student's work, so
ADR 0003 — that Submission text is never stored — is untouched.

## Consequences

Prompt size grows by the size of the documents on every call, for every student in a class. Source
material is therefore capped at roughly 50.000 characters per Assignment, enforced at upload with a
clear error, so that the cost of an evaluation is bounded by something the Educator can see and
control. Documents are markdown or plain text only; PDF extraction is separate work, and is more
valuable applied to Submissions first.

Retrieval was rejected: selecting relevant passages from the two or three documents an Assignment
actually has is machinery in search of a problem.
