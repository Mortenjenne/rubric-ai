# 08: Prompt assembly from an Assignment version

**What to build:** Rework `PromptBuilder` so it assembles prompts from an Assignment version instead
of the one global Rubric. The code keeps the output contract; the Educator's Assessment stance is
appended to the system prompt; Source material goes into the user prompt above the Rubric. See
ADR 0006 for why the split falls exactly where it does.

**Blocked by:** 06, 07

**Status:** ready-for-agent

- [ ] `PromptBuilder.build(...)` takes an Assignment version and a Submission text.
- [ ] The system prompt is: the code-owned output contract, then the version's Assessment stance,
      clearly delimited. The stance is never placed in the user prompt.
- [ ] The code-owned contract retains, unchanged in substance: the advisory framing; the instruction
      to place each Criterion at the Level whose descriptor fits, using only the Rubric's own
      descriptors; the verbatim-evidence rule; the whole `SVARFORMAT` JSON block built from
      `Level.values()` and `SuggestedGradeValue.values()`; and the requirement that all output text be
      Danish.
- [ ] The AP-level calibration, the judge-by-substance-not-keywords rule and the
      weaknesses-are-about-content rule are **removed** from `PromptBuilder` — they now arrive as
      stance from the praktikrapport template (issue 04).
- [ ] A blank stance produces a valid prompt with no empty section and no dangling header.
- [ ] The user prompt is: Source material (omitted entirely when there is none), then the Rubric, then
      the Submission text. Section headers are assignment-neutral — no `PRAKTIKRAPPORT`, no `Opgave`
      that assumes a report.
- [ ] `LlmRequest`'s javadoc is corrected: it currently describes an English system prompt, which has
      not been true since the prompts were rewritten in Danish.
- [ ] The existing `PromptBuilderTest` is extended rather than replaced, covering: stance appears in
      the system prompt and not the user prompt; Source material precedes the Rubric; a Criterion's
      Source references are rendered; and a blank stance and empty Source material both produce clean
      output.
