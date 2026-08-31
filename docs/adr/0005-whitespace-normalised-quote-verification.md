# Evidence quotes are compared with whitespace normalised, nothing else

Every evidence quote in a model payload is checked against the Submission text before an Evaluation
is trusted: a fabricated quote is worse than none, because it looks checkable and isn't. The sample
praktikrapporter carry markdown artefacts — table pipes, footnote markers, and page-wrap line breaks
that split a sentence mid-word — so a model copying a quote that crosses one of these can reproduce
the underlying words exactly while differing only in whitespace. Byte-exact comparison would reject
that as fabricated even though it is an honest quote.

We decided: before comparing, collapse runs of whitespace (spaces, tabs, newlines) to a single space
and strip both ends, on both the quote and the Submission text, then require the quote to be a
literal substring of the normalised Submission. Nothing else is normalised — no case-folding, no
punctuation or markdown stripping — because widening the match further starts trading away the
guarantee that makes a Finding checkable in the first place.

## Consequences

A quote that only differs from the source by whitespace is accepted; a quote that paraphrases,
reorders words, or drops/adds punctuation is still rejected. This is deliberately narrow — if a future
sample corpus turns out to need more (e.g. curly vs straight quotation marks), that is a new decision,
not an extension of this one.
