# Rubric AI

Backend service that helps educators on the Danish Datamatiker (AP in Computer Science) programme
assess student work: a Submission is judged against a fixed Rubric by a large language model, which
returns a structured, advisory Evaluation — never an authoritative mark.

## Language

**Assignment**:
The task the school sets, identical for every student on the course: the 5th-semester
praktikrapport, a reflection on the student's internship that grounds the final oral exam. Owns the
source material and the rubric.
_Avoid_: Opgave (when writing English), report, brief

**Submission**:
One student's document, handed in as an answer to an Assignment. Markdown for now, PDF later.
_Avoid_: Assignment, report, paper, upload

**Source material**:
The course documents an Assignment is defined by — its formal requirements, its learning goals, its
values. What the Rubric was derived from; never sent to the model to judge a Submission directly.
_Avoid_: Sources, docs, criteria

**Rubric**:
The assessment matrix a Submission is judged against: six Criteria, each with the same four Levels.
Shaped once by hand from Source material before the service runs — not produced per request.
_Avoid_: Vurderingsmatrix (when writing English), grading scheme, template

**Criterion**:
One row of a Rubric — a single aspect of a Submission that is judged on its own, traceable to the
Source material it came from.
_Avoid_: Kriterie (when writing English), rule, requirement, check

**Level**:
One column of a Rubric — a named band of performance: Mangelfuldt, Acceptabelt, Tilfredsstillende,
Udmærket. A Level names a quality, never a grade.
_Avoid_: Grade, score, band, rating

**Evaluation**:
The result of judging one Submission against the Rubric: an overall assessment, one Finding per
Criterion, and questions to put to the student. Advisory — a starting point for the Educator's own
judgement, not a verdict.
_Avoid_: Assessment, grading, result, bedømmelse

**Evaluation summary**:
The compact, list-friendly view of an Evaluation: its identity, provenance (Rubric version, provider,
model), timestamp, and Suggested grade — everything except the Findings and narrative content. Used
where many Evaluations are shown at once; the full Evaluation is fetched by id when the detail is
needed.
_Avoid_: Compact evaluation, evaluation list item

**Finding**:
An Evaluation's verdict on one Criterion: the Level reached, the strengths and weaknesses behind it,
and what would improve it.
_Avoid_: Feedback, comment, score

**Suggested grade**:
A single mark on the 7-trins-skala offered alongside an Evaluation. Advisory only, and the one part
of the output an Educator is expected to overrule.
_Avoid_: Grade, karakter, final grade

**Weight**:
The share of the whole an Educator considers a Criterion worth. Guidance on emphasis, not a factor in
any calculation — nothing multiplies by it.
_Avoid_: Score, points, percentage

**Educator**:
The teacher who owns an Assignment and is the only reader of an Evaluation. Students never use this
system and never see its output unmediated.
_Avoid_: Teacher, examiner, censor, user
