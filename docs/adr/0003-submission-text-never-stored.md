# Submission text is never written to storage

Evaluations are persisted — grade, provider, model, rubric version and findings — but the student's
submitted text is held in memory for the duration of the request and then discarded. A praktikrapport
names the student's employer, their client, named colleagues, mentor feedback and, in at least one of
our sample reports, personality-test results. Not storing it is data minimisation by design, and it
costs us only the ability to re-run an old evaluation from the archive.

## Consequences

Findings quote the submission verbatim, so short excerpts do persist inside the stored evidence. That
is deliberate — the quotes are what make a finding checkable — but it means the store is not entirely
free of student text, and the retention story should say so rather than claim otherwise.
