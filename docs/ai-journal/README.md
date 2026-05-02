# AI Usage Journal — CSE 341 Project

This folder holds my AI Usage Journal entries (deliverable D4).

## Rules I must follow (from the handout §6)
- Every entry must carry a **real date** at the time of the session. Backfilled journals receive zero.
- Part 1 journal: ≥ 4 substantive entries dated up to **2026-05-08**, must include **Experiment E1**.
- Part 2 journal: ≥ 6 additional entries dated **2026-05-09 to 2026-05-22**, must include **Experiments E2 and E3**.
- Each entry is its own file in `entries/` named `NN_YYYY-MM-DD_short-topic.md`.
- The reflection sections must be in my own voice — no AI-generated text pasted in unless quoted and discussed.
- At least 3 entries across the project must document cases where the AI was wrong, unclear, or suboptimal.

## The three mandatory experiments
- **E1** — Ask the AI to generate an EBNF grammar from a one-paragraph description of my language. Compare with mine. (Part 1)
- **E2** — Ask the AI to explain name vs structural equivalence for my record type. Check against Sebesta §6.14. (Part 2)
- **E3** — Ask the AI to implement my type checker (or a non-trivial portion). Run on ≥ 3 known-answer inputs, report bugs. (Part 2)

## Workflow
1. After any substantive AI session, copy `entry_template.md` into `entries/` with a numbered, dated filename.
2. Fill it in immediately while the session is fresh — especially the **Accepted / Rejected / Errors caught / Reflection** fields.
3. At submission time, concatenate entries into a single PDF named `<studentID>_D4_P1.pdf` (or `_P2`).

## Folder layout
```
ai-journal/
  README.md            (this file)
  entry_template.md    (copy this for each new entry)
  entries/             (one .md per session)
  experiments/         (E1, E2, E3 evidence — raw prompts + responses)
```
