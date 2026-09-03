# Rubric AI

Backend service that helps a Danish educator assess student work: an Educator configures an
Assignment — its Rubric, its Assessment stance and its Source material — and a Submission handed in
against that Assignment is judged by a large language model, which returns a structured, advisory
Evaluation — never an authoritative mark.

## Language

### The Assignment and how it is configured

**Assignment**:
The task a school sets, owned by one Educator and identical for every student who answers it. Owns
the Rubric a Submission is judged against, the Assessment stance it is judged with, and the Source
material it was derived from.
_Avoid_: Opgave (when writing English), course, brief, assessment

**Assignment version**:
A frozen snapshot of everything about an Assignment that the model is shown: its Rubric, its
Assessment stance and its Source material. Published once by the Educator and never edited
afterwards, so that two Evaluations carrying the same version are known to be comparable.
_Avoid_: Rubric version, revision, snapshot, release

**Draft**:
The one editable state of an Assignment. Always present, never frozen, and always what the next
published Assignment version is made from.
_Avoid_: Working copy, unpublished version, staging

**Assessment stance**:
The Educator's own framing of how their Assignment should be judged: the level to expect of the
programme, what counts as evidence of a Criterion, what a weakness may and may not be about. Free
prose, written by the Educator, and the only part of the model's instructions they control.
_Avoid_: Prompt, system prompt, instructions, guidelines

**Template**:
A starting Assignment shipped with the service, owned by no Educator and never evaluated against.
Copied into an Educator's own Assignment so that a new Assignment begins from a worked example
rather than an empty form.
_Avoid_: Example, preset, default rubric, sample

**Source material**:
The course documents an Assignment is defined by — its formal requirements, its learning goals, its
values. Uploaded by the Educator, frozen into each Assignment version, and given to the model as
context for the Assignment it is judging against.
_Avoid_: Sources, docs, attachments, uploads

**Source reference**:
A Criterion's note of which Source material it was derived from. A pointer written by the Educator,
not the document itself — what makes a Criterion defensible to a colleague or a censor.
_Avoid_: Source material, citation, provenance

**Rubric**:
The assessment matrix a Submission is judged against: an Assignment's Criteria, each with the same
four Levels. Authored by the Educator and frozen into an Assignment version — never produced per
request.
_Avoid_: Vurderingsmatrix (when writing English), grading scheme, template

**Criterion**:
One row of a Rubric — a single aspect of a Submission that is judged on its own, carrying a Source
reference to the Source material it came from.
_Avoid_: Kriterie (when writing English), rule, requirement, check

**Level**:
One column of a Rubric — a named band of performance: Mangelfuldt, Acceptabelt, Tilfredsstillende,
Udmærket. The same four for every Assignment, in every subject. A Level names a quality, never a
grade.
_Avoid_: Grade, score, band, rating

**Weight**:
The share of the whole an Educator considers a Criterion worth. Guidance on emphasis, not a factor in
any calculation — nothing multiplies by it, and nothing requires the Weights to sum to anything.
_Avoid_: Score, points, percentage

### The assessment

**Submission**:
One student's document, handed in as an answer to an Assignment. Markdown for now, PDF later.
_Avoid_: Assignment, report, paper, upload

**Evaluation**:
The result of judging one Submission against one Assignment version: an overall assessment, one
Finding per Criterion, and questions to put to the student. Advisory — a starting point for the
Educator's own judgement, not a verdict.
_Avoid_: Assessment, grading, result, bedømmelse

**Evaluation summary**:
The compact, list-friendly view of an Evaluation: its identity, provenance (Assignment, Assignment
version, provider, model), timestamp, and Suggested grade — everything except the Findings and
narrative content. Used where many Evaluations are shown at once; the full Evaluation is fetched by
id when the detail is needed.
_Avoid_: Compact evaluation, evaluation list item

**Finding**:
An Evaluation's verdict on one Criterion: the Level reached, the strengths and weaknesses behind it,
and what would improve it.
_Avoid_: Feedback, comment, score

**Suggested grade**:
A single mark on the 7-trins-skala offered alongside an Evaluation. Advisory only, and the one part
of the output an Educator is expected to overrule.
_Avoid_: Grade, karakter, final grade

### People

**Educator**:
The teacher who owns an Assignment and is the only reader of an Evaluation produced against it. An
Educator never sees another Educator's Assignments or Evaluations. Students never use this system and
never see its output unmediated.
_Avoid_: Teacher, examiner, censor, user
