# 04: Reject output the schema can't catch

**What to build:** The checks that make an Evaluation trustworthy rather than merely well-formed. A
payload can satisfy every type constraint and still be wrong: quoting sentences the student never
wrote, skipping a Criterion, inventing one, naming a Level that isn't in the Rubric, or returning two
dialogue questions instead of five.

The Educator's whole reason for trusting a Finding is that it quotes the Submission — a fabricated
quote is worse than no quote, because it looks checkable and isn't. So every evidence entry is
verified against the submitted text, and a payload that fails verification is rejected outright rather
than returned with the bad quote stripped out.

When validation fails, the model is asked once more before the request gives up. One re-ask, not a
loop.

Note for whoever picks this up: the sample reports carry markdown artefacts — image placeholders,
table pipes, footnote markers — so a model quoting across them can produce a near-miss that is a fair
quote of the underlying text. Decide deliberately whether comparison normalises whitespace before
matching, and write the decision down; strict byte equality may reject honest quotes.

**Blocked by:** 02

**Status:** ready-for-agent

- [x] Every evidence entry is verified as a verbatim excerpt of the submitted text; a payload containing a fabricated quote is rejected
- [x] The normalisation rule used when comparing quotes is decided deliberately and documented
- [x] A payload missing a Criterion is rejected
- [x] A payload containing a Criterion that is not in the active Rubric is rejected
- [x] Findings are returned in Rubric order
- [x] A Level name not drawn from the Rubric is rejected
- [x] Fewer than four or more than six dialogue questions is rejected
- [x] A rejected payload triggers exactly one re-ask of the model; a second failure ends the request
- [x] Nothing is persisted when validation ultimately fails
- [x] Each rejection rule is covered by a test driving the endpoint with a canned payload through the fake port
