Date: 17.05.2026
Model: Claude Opus 4.7 (in-IDE, full project context)
Session link: (in-IDE Claude Code session, not a web share — full transcript captured in `entries/09_2026-05-17_E3-type-checker-implementation.md`)

# E3 — AI implements the §4.5 type checker

The mandatory E3 experiment per `ai-journal/README.md`: have an AI implement a non-trivial portion of the language implementation, test it on ≥ 3 known-answer inputs, and report any bugs introduced. Setup deliberately differs from E1/E2 — here the AI has full project context (D1 spec, parser, AST), because a context-free type checker would not be a useful artifact for this project.

## Prompt

Not a single-shot prompt; the experiment was conversational. The closest equivalent to a "prompt" is the combination of:

- `docs/design/D1.md` §4.5 (the static type system: closed type universe, family table, slot rules, expression rules, §4.5.8 pseudocode for the single-pass checker)
- The already-implemented `src/fitlang/Parser.java` + `src/fitlang/ast/Ast.java`
- The instruction: *"Implement the §4.5 type checker as a single bottom-up pass. Collect diagnostics rather than throw on first failure. Wire it into `Main` as a phase between parser and interpreter, behind a `--no-check` escape hatch."*

I did not type any of the type-checker code. I directed the design, reviewed the output, and ran the test programs.

## Artifact produced

- `src/fitlang/TypeChecker.java` — 324 lines as committed, single bottom-up pass, no scoping stack, modelled directly on §4.5.8's pseudocode plus the slot-family checks from §4.5.3, §4.5.5, §4.5.6.
- Type representation: three classes (`TNumber` singleton, `TQuantity { UnitFamily fam }`, `TRate { UnitFamily numer, denom }`) with value-semantics `equals`/`hashCode` so the `applyBinary` switch compares types directly.
- Binary operator dispatch (`applyBinary`) implementing §4.5.4–§4.5.5 case-by-case: `+`/`-` allow Number+Number and same-family Quantity+Quantity; `*` allows Number×Number, Number×Quantity (commutative), and Rate(D,T) × Quantity(T) (commutative, family-relabelling); `/` allows Number/Number and Quantity/Number (same-family Quantity/Quantity is rejected per §4.5.4). Rate+Rate, Mass×Mass, Mass/Mass all return `null` and produce an "operator '⊕' is not defined on T1 and T2 (§4.5.4–§4.5.5)" diagnostic.
- `--no-check` flag added to `Main` so D3's intentionally ill-typed programs can still be `--dump-ast`-ed for inspection.
- UTF-8 console fix in `Main.main` (`System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8))`) because `§` was rendering as `�` on the Windows cp1252 default.

## Known-answer test set

Five distinct programs, well clear of the ≥ 3 minimum required by the experiment. Coverage spans every §4.5 rule plus the §4.4.9 mode/direction consistency rule.

| # | Program | Expected verdict | Actual verdict |
|---|---|---|---|
| 1 | `samples/valid_02_workout_only.fl` | well-typed | OK ✓ |
| 2 | `samples/valid_03_diet_only.fl` | well-typed | OK ✓ |
| 3 | `samples/_p2_expr_smoke.fl` — exercises the §4.7 expression layer (`*`, `/`, parens, unary `-`, rate atoms, Rate × Time) | well-typed | OK ✓ |
| 4 | `samples/_typeerr_cross_family.fl` — 4 deliberate errors | 4 rejections | 4/4 caught: `Mass + Energy`, `Mass / Mass`, `Mass × Mass`, unbound identifier ✓ |
| 5 | `samples/_typeerr_rate_and_slots.fl` — 6 deliberate errors | 6 rejections | 6/6 caught: exercise weight is Energy, progress rate denominator wrong family, macro is Energy, per-unit wrong, goal-rate is Rate(Mass,Mass), unknown meal label ✓ |

Programs 4 and 5 were later merged into `samples/invalid_typeerror.fl` (the D3 type-error sample), with numbered comments mapping each of the 10 rejections to the §4.5 / §4.4 rule it violates.

### Bonus diagnostic (not a checker bug, but a real catch)

Re-running on `samples/valid_01_full.fl` after the checker was wired in surfaced a **bulk + lose** header/goal-rate inconsistency that had been in the repo since Part 1. The checker correctly rejected it per §4.4.9. The fix went into the sample (`lose` → `gain`), not the checker.

## Bugs introduced by the AI

**Zero.** All five programs returned the expected verdict on the first compile.

This is a stronger result than the experiment design technically requires, and two confounds are worth flagging before drawing any "AI is reliable for this" conclusion:

1. **The checker is small (324 lines) and structurally simple.** One method per node kind, no inference beyond syntactic dispatch. The §4.5.8 pseudocode in D1 is essentially the algorithm spelled out — Claude was transcribing a spec, not synthesising one.
2. **The test set is directed at rules I knew Claude implemented.** Errors of omission — rules in §4.5 that Claude didn't implement and that the test programs don't exercise — would not be caught by this methodology. A more adversarial test set (fuzzing, or one written by someone who hadn't read the implementation) would be a stronger check.

## One design call the AI made on its own

§4.4.10 says `<when-decl>` is statically dispatched and the surviving branch is "spliced" into the rules section. Claude's first draft type-checked **both** branches against a snapshot of the env and scoped any let-bindings introduced inside a branch to that branch only — a *defensible* reading of "both branches must be well-typed even if only one runs" (Java's `if (false) { ... }` is type-checked).

I overrode this after the rest of the entry was drafted. The agreed reading is **splice-and-leak**: pick the surviving branch via the athlete header `goal`, type-check only that branch, let its bindings extend the env for the rules that follow. Matches §4.4.10's literal "splice" wording, accepts the Sebesta §8.1 static-if consequence that type errors in the dead branch are not compile errors. Claude rewrote `checkWhenDecl` accordingly; all 5 programs above still pass / fail as expected after the change.

## What I learned

Conversational AI implementation works when the spec is detailed enough to be transcribed — the §4.5.8 pseudocode left almost no design freedom, and the checker came out clean. Where the spec leaves *interpretive* room (the when-decl branch scoping), the AI made a choice without flagging it; I caught this only because I re-read the file. For E3 specifically: AI replaced ~3 hours of mechanical transcription, but the *design* of §4.5 (the table, the rule set, the closed type universe) was already mine, and that is where the actual language work lives.
