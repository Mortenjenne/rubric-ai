# How this was built

This repo is two things at once: a solution to a school assignment, and an experiment in building
that solution **agentically** — with Claude Code driving, using the open skill set Matt Pocock
publishes at [aihero.dev](https://www.aihero.dev) ([`mattpocock/skills`](https://github.com/mattpocock/skills)).

[`README.md`](README.md) covers the task and the architecture; this file covers the process.

## The task

**AI-vurdering af en opgave ud fra en rubric** — a one-day assignment on the 4th semester of the
Datamatiker programme. The brief: take a real student hand-in, derive an assessment rubric from the
course material, and build something that uses an LLM to produce a *vejledende* (advisory)
assessment against that rubric. Explicitly not an automatic grader.

We chose the 5th-semester **praktikrapport** as the assignment being judged. The five sub-tasks were:
derive the rubric, design the prompts, build the backend, return structured feedback, and test and
reflect on whether the result is actually usable.

What came out of it is `rubric-ai`: a Spring Boot service that judges a submission against a
six-criterion rubric and returns a structured, advisory evaluation in Danish — plus a React frontend
in a sibling repo. The reflection on the *result* — what the rubric caught, where the model was weak,
what we'd change — is a post at **[www.corral.dk](https://www.corral.dk/)**, not a file in this repo.

## The experiment

The rule we set ourselves was: **don't open the editor first.** Every phase of the work goes through
a skill, and every phase leaves a written artifact behind that the next phase reads.

```
/grill-with-docs  →  CONTEXT.md + docs/adr/     shared understanding
      ↓
/to-spec          →  .scratch/<feature>/spec.md  45 user stories + an Out of Scope list
      ↓
/to-tickets       →  .scratch/<feature>/issues/  six numbered tickets
      ↓
/implement        →  one ticket, start to end    drives the three steps below
      ↓
  /tdd            →  src/                        tests first, at pre-agreed seams
  full suite      →  green                       before anything is called done
  /code-review    →  fixes                       against the standards and against the spec
      ↓
  commit          →  one ticket per commit
```

`/implement` is the one that ties the back half together: hand it a ticket and it runs TDD at the
seams we agreed, keeps the suite green, calls `/code-review` on its own work, and commits. That's
why the git history reads one ticket per commit rather than one afternoon per commit.

The point of the chain is that context is **written down, not remembered**. Each step's output is a
file in the repo, so the next step (and the next session, and a human) reads the same thing the agent
does.

### Setup

`/setup-matt-pocock-skills` installed the skill set and wired it to this repo:

| Path | What it is |
| --- | --- |
| [`.agents/skills/`](.agents/skills/) | The vendored skills themselves — 25 of them |
| [`skills-lock.json`](skills-lock.json) | Pins each skill to its source path and content hash |
| [`CLAUDE.md`](CLAUDE.md) | The repo's agent instructions — points at the three docs below |
| [`docs/agents/issue-tracker.md`](docs/agents/issue-tracker.md) | Issues are local markdown under `.scratch/`, not GitHub |
| [`docs/agents/triage-labels.md`](docs/agents/triage-labels.md) | `needs-triage` / `ready-for-agent` / `ready-for-human` / … |
| [`docs/agents/domain.md`](docs/agents/domain.md) | Domain docs live in `CONTEXT.md` + `docs/adr/` |

Those three `docs/agents/` files are the adapters: the skills ask for "the issue tracker" or "the
glossary", and this repo answers with local files instead of a hosted tool. That's why the specs and
tickets sit in `.scratch/` and are committed alongside the code.

### The one that mattered

`/grill-with-docs` — a thin wrapper that runs `grilling` (a relentless interview about the plan) and
`domain-modeling` (write the vocabulary and the decisions down as they surface) together. Being
interrogated *before* writing code forced decisions we'd otherwise have made accidentally three hours
in, by whatever the first implementation happened to do:

- Is the suggested grade computed from the levels, or emitted by the model?
- Do we store the submission text?
- What happens when the model fabricates a quote?
- Is a 401 the same kind of failure as a 429?

Each answer landed in [`CONTEXT.md`](CONTEXT.md) (twelve terms, each with an explicit *avoid* list) or
in [`docs/adr/`](docs/adr/) (five decision records, each naming the option it rejected). Written down,
they propagate: *"a Level names a quality, never a grade"* is one glossary line that shows up as a
constraint in the rubric JSON, the JSON schema, the API docs and the UI — six hours later, without
anyone having to remember it.

## What the artifacts look like now

| Artifact | Produced by | Lives in |
| --- | --- | --- |
| Domain glossary | `/domain-modeling` | [`CONTEXT.md`](CONTEXT.md) |
| 5 architecture decision records | `/domain-modeling` | [`docs/adr/`](docs/adr/) |
| Feature specs | `/to-spec` | [`.scratch/*/spec.md`](.scratch/) |
| 9 implementation tickets | `/to-tickets` | [`.scratch/*/issues/`](.scratch/) |
| The code, 37 tests | `/implement`, driving `/tdd` | [`src/`](src/) |
| Package restructuring | `/codebase-design` | commit `e2ebe18` |
| Vocabulary + standards fixes | `/code-review` | commits `324e9c5`, `91957f5` |
| The reflection | by hand | [www.corral.dk](https://www.corral.dk/) |

The git history reads as the process: `Boot service on Postgres with Rubric v1 seeded` →
`Evaluate a Submission end-to-end against a fake language model` → `Call OpenAI for real behind the
language model port` → `Close ticket 04: reject model output the schema can't catch`. One ticket per
commit, in spec order.

## Honest notes on the experiment

**What worked.** Front-loading the disagreements. When we later hit a wall — every OpenAI tier from
`gpt-5.5` up rejects `temperature: 0`, which our determinism requirement needs — the trade-off
resolved in a minute instead of a debate, because ADR 0002 had already written down *why*
determinism mattered. Same with the glossary: the "avoid" lists caught real vocabulary drift in
review (`324e9c5`), where code had started calling an Evaluation a "result".

**What cost time.** The interview is genuinely slow, and it feels like procrastination while you're
in it. On a one-day assignment that's a real bet. It paid here because the domain has a sharp
constraint at its centre (*advisory, never authoritative*) that everything else has to respect — a
CRUD app would not have earned it.

**What we'd do differently.** Run `/code-review` per ticket rather than in batches, and treat
`.scratch/` specs as living documents — ours drifted from the code once the frontend started
proposing endpoints back to the backend.

**The takeaway.** The deliverable of an agentic session isn't only the code. It's the code *plus* the
written understanding that produced it — and the second half is what makes the next session, or the
next person, able to answer *"why does it do that?"* for every part of it.
