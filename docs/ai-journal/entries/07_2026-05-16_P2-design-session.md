---
entry: 07
date: 2026-05-16
phase: Design — D1 P2 sections (§4.4 Semantics, §4.5 Type System, §4.7 Expressions, §4.8 Rationale)
ai_tool: Claude Opus 4.7
---

# Entry 07 — Part 2 design kickoff: type system, semantics, multi-week schedule

## Goal

Settle the four P2 design sections in D1 — semantics (§4.4), type system (§4.5), expressions/assignment (§4.7), rationale (§4.8) — and write them into `docs/design/D1.md` so the type checker and interpreter in D2 have a fixed spec to implement against. Also resolve three open design calls that P1 deferred: type equivalence rule for records, what `let` can bind, and whether the schedule covers one week or a multi-week horizon.

## Prompt

Open-ended working session, not a single prompt. The session shape was: Claude proposed a starting point (spec-first vs. code-first vs. plan-the-week), I picked spec-first, then we worked through the three open design calls one at a time before drafting the sections. Key turns where Claude posed a concrete choice:

1. Type equivalence for records (name vs. structural vs. hybrid).
2. What `let` can bind (scalars+quantities only; +rates; or also exercises/meals).
3. Multi-week vs. single-week schedule (single-week; `plan N week` with default 1; per-week named blocks).

After each call was made, Claude drafted the section directly into `D1.md`. After §4.5 and §4.4 were drafted Claude listed the design calls it had baked in and asked for confirmation before continuing.

## Response (key parts)

### Type equivalence — name vs. structural

Claude's framing: in FitLang, structural and name equivalence collapse to the same answer for built-in records because the grammar admits exactly one schema per kind keyword (`exercise` has exactly `sets, reps, weight`; `meal` has only `protein, carbs, fat`). The meaningful distinction would only appear if users could define their own schemas — they can't. Claude recommended name equivalence with the one-sentence defence "two values are the same type iff they were built from the same declared schema kind."

### `let` bindings

Claude laid out three options:
- A: scalars + quantities only (Sebesta §5.8 named constants).
- B: also bind exercises/meals — Claude warned this duplicates `routine` and the string-label space.
- C: also bind rates — cheap extension of A.

Claude recommended **A + C** with the defence "`let` introduces named constants for numeric values; everything that has a label in the domain already gets one in the string-label space."

### Multi-week vs. single-week schedule

Claude initially drafted §4.4 with single-week output (`WeeklyPlan` denotation, `progress` attaches `nextWeekWeight` per exercise). I asked whether long-term programs should be allowed. Claude laid out:
- A: keep single-week.
- B: `plan N week with ...`, default 1; per-week loop projects bodyweight + exercise weights + target thresholds.
- C: explicit per-week named blocks (rejected as overkill — duplicates `progress`).

Claude recommended **B** on the grounds that without a multi-week horizon, `progress` and `goal lose/gain` exist only to demonstrate `Rate × Time` without actually using it.

### Drafted sections

After the three calls were made, Claude wrote (in `docs/design/D1.md`):
- §4.5 in 8 subsections: type universe, strong typing stance, name equivalence, within/across-family coercion rules, rate algebra (`Rate × Time → Quantity`), record kinds table, `let` admissibility, type-checker pseudocode.
- §4.4 in 11 subsections: operational big-step style, `Plan { athlete, weeks: [Week] }` denotation, atomic forms with canonicalization, expression rules, let-environment threading, per-week EXERCISE rule, PROGRESS as `progressRate` binder, USE for routines, `plan N week` PLAN rule, per-week TARGET, static `when` pre-pass, worked example.
- §4.7 in 5 subsections: precedence-stratified EBNF (additive/multiplicative/unary/atom), precedence table with Sebesta analogues, left-to-right operand order (unobservable due to purity), the "no assignment statement" stance (`=` is binding, `:` is field init), expression type rules summary.
- §4.8: ten paragraphs, one per major decision (closed lexicon, strong typing, name equivalence, single-assignment, static scoping, multi-week schedule, static `when`, no user types, two name spaces, tree-walking Java interpreter), each naming the trade-off in Sebesta vocabulary.

Also patched: §4.3.2 schedule production updated to `<schedule-stmt> → "plan" [ NUM_LIT ] "week" "with" <schedule-clauses>` (LL(1) preserved — disjoint look-ahead `NUM_LIT` vs `"week"`).

## Accepted

- **Name equivalence** for record kinds. Picked over structural because the kind keyword is the carrier of identity; the two rules agree on every legal FitLang program, so we picked the one with the simpler one-sentence defence.
- **`let` binds Number, Quantity, Rate** (Option A + C). Records stay in the string-label space; no second name space for declarations that already have labels.
- **Multi-week `plan N week`** (Option B). Default 1 when the integer is omitted, so the existing `plan week ...` syntax stays legal. Per-week loop projects bodyweight under `goalRate`, exercise weights under `progressRate`, and re-evaluates `target` thresholds.
- **Meals constant across weeks.** A lifter who wants per-week meal changes writes new meal declarations; this leaves room for a future `<when-decl>` keyed on `week index`.
- **`when` is fully static** — pre-pass eliminates the dead branch before the semantics rules run, because the dispatch value is the literal goal mode from the header.
- **`per UNIT of bodyweight` is sugar**, not a general operator. The only carve-out from the §4.5.4 no-`Mass × Mass` rule; defended by analogy to Ada `'Pos`.
- **No assignment statement.** `=` in `<let-decl>` is binding (ML-style); `:` in record literals is field initialisation (Ada aggregate-style).
- **Operator precedence table**: unary − > * / > + − > relational, with relational non-associative (so `a < b < c` is not writable — matches the one-`<rel-op>`-per-target-line grammar).

## Rejected / modified

- **Option B for `let` (binding exercises/meals).** Rejected because it duplicates `routine` (parameterised exercise reuse) and the string-label space (workout/meal/routine identity).
- **Option C for the schedule (named per-week blocks).** Rejected as overkill — `progress` already expresses week-over-week change; named per-week blocks would remove the reason `progress` exists.
- **Claude's initial single-week §4.4 draft.** Replaced after the multi-week decision: §4.4.2 denotation became `Plan { athlete, weeks }`, §4.4.6 EXERCISE rule became per-week with `weight = base + progressRate × (k−1) week`, §4.4.8 PLAN rule became an N-iteration loop, §4.4.9 TARGET became per-week with `bw_k`, §4.4.11 worked example rewritten as a four-week bulk block.
- **`goal lose/gain` computing calorie balance** (would need a magic constant like 1 kg fat ≈ 7700 kcal). Rejected; the rate only drives bodyweight projection and is consistency-checked against the header `goal` mode (e.g., `bulk` + `lose` is a type error).
- **Same-family `*` and `/`** (`kg * kg`, `kg / kg`). Rejected because they'd open the closed type universe — `Mass²` and dimensionless `Number` from `Mass`-division have no inhabitants in §4.5.1.

## Errors I caught

- The §4.1 sample program has `goal: bulk` in the header but `goal lose 0.5 kg/week` in the rules section. Claude noticed mid-draft and added a parenthetical to the §4.4.11 worked example explaining this is a type error and the interpreter never runs it; then the worked-example was rewritten with `goal gain 0.25 kg/week` so the projection actually executes.
- The first §4.4.6 EXERCISE rule (single-week version) talked about `nextWeekWeight` as an annotation. After the multi-week switch this would have been dead — Claude correctly replaced the annotation with the per-week parameterised rule rather than leaving both forms in the spec.

## Reflection

Today we looked at let and sheduling. We aggreed to keep the let as is but for weekly plans we added the ability to plan multi weeks plan. This is better than the original behaviour (one plan
per wekk).
