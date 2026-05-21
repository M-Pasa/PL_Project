---
entry: 16
date: 2026-05-21
phase: Writing / Testing
ai_tool: Claude Opus 4.7 (Claude Code CLI)
---

# Entry 16 — P2 submission packaging and handout audit

## Goal

Build the P2 artifacts, fix whatever the build does not render cleanly,
refresh the README, audit against the handout, add a transparency note
for the public repo.

## Prompt

Conversational, no single canonical prompt. Substantive turns:

- *"we gotta prepare the submission folder"*
- *"I see text out of the bound of the page in D1"*
- *"check the raw sessions, I am sure that there are things we left"*
- *"how about the experiments are we done with them?"*
- *"I want you to go again and check the handout file"*

## Response (key parts)

- `preprocess.py` extended to cover the math glyphs used in §4.4 (⊢, ⇓,
  ⇝, ↦, ⊕, ⊥, ∈, ∪, ×, Σ, subscripts).
- `header.tex` (new) redefines base `verbatim` with fvextra `breaklines`
  + `breakanywhere`; `build.sh` passes it via pandoc `-H`.
- Root `README.md` rewritten for P2 (flags, error phases, full sample
  inventory).
- `ai-journal/README.md` gained an "AI use, in plain terms" section;
  root README links to it.
- `experiments/E3/claude_opus_4.7.md` added as the structural mirror of
  `E1/` and `E2/` (E3 itself was done 2026-05-17 / entry 09).
- Handout audit: clean against D1 §4.1–§4.8, D2, D3, D4, D5.

## Accepted

- The verbatim line-wrap fix.
- The README P2 rewrite — old one would have shipped a P1 title inside
  the P2 zip.
- The split into three commits with the E3 commit explicitly noting it
  is a structural mirror, not new experimentation.

## Rejected / modified

- First fvextra attempt only redefined `Highlighting`. Plain ``` blocks
  still overflowed because they render as base `verbatim`, not
  `Highlighting`. Replaced with a `\renewenvironment{verbatim}`.
- "Claude Code in-IDE" in the transparency note — corrected to CLI.

## Errors I caught

- Preprocessor coverage was guessed, not enumerated. First build failed
  on ⊢; a one-glyph patch would have failed again on the next. Forced a
  grep of every non-ASCII codepoint across the P2 docs before re-running.
- The deferred README update was in no "next" list anywhere. Only
  surfaced when I told the AI to grep the previous raw session log for
  "later" / "leftover".
- The `experiments/E3/` folder was missing. Caught because I asked, not
  from any plan.

## Reflection

Packaging is where the easy points go. Three of today's catches
(wrong-part README, missing experiments folder, broken rendering) would
not have shown up in the build script's "Done." line. Cross-check
against the handout has to be the final step, not a sanity pass.
