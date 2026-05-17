---
entry: 09
date: 2026-05-17
phase: Type checker — E3 experiment (AI implements §4.5 type checker)
ai_tool: Claude Opus 4.7
---

# Entry 09 — E3: AI-implemented type checker, tested on five known-answer inputs

## Goal

Run the mandatory E3 experiment: have an AI implement a non-trivial portion of the language implementation (in our case the full §4.5 type checker), test it on at least three known-answer inputs, and report any bugs the AI introduced. Setup differs from E2 by design: here the AI has full project context (D1 spec, parser, AST), because a context-free type checker would not be a useful artifact for the project.

## Prompt

Conversational, not single-shot. The session shape was: I asked Claude to start the type checker (after we had agreed in this same session to extend the parser/AST to the §4.7 expression layer first). Claude:

1. Proposed the order — extend parser/AST to full §4.7 expressions, then write the checker, then wire it into `Main`, then build the type-error programs — and asked me to confirm.
2. Wrote the parser/AST extension (`UnaryExpr`, `RateExpr`, full `parseAdd → parseMul → parseUnary → parseAtom` chain with the rate-vs-divide k=2 peek inside `parseAtom`), then ran an expression smoke file before moving on.
3. Wrote `src/fitlang/TypeChecker.java` from scratch — 301 lines, single bottom-up pass, no scoping stack, modelled directly on §4.5.8's pseudocode plus the slot-family checks from §4.5.3, §4.5.5, §4.5.6.
4. Wrote two type-error programs (`_typeerr_cross_family.fl`, `_typeerr_rate_and_slots.fl`) to exercise every rejection path.

No single canonical prompt to paste — the closest equivalent is the §4.5 section of `D1.md` plus the AST and the running parser. The "AI-implements" criterion is that I did not type any of the type-checker code; I directed the design and reviewed the output.

## Response (key parts of what Claude produced)

### Type representation

A small sealed-style hierarchy in `TypeChecker.java`:

```java
abstract class FType { }
class TNumber   extends FType { static INSTANCE; }            // singleton
class TQuantity extends FType { UnitFamily fam; }
class TRate     extends FType { UnitFamily numer, denom; }
```

`equals`/`hashCode` overridden so type comparisons in `applyBinary` use value semantics. This matches §4.5.1's closed type universe verbatim — three categories, parametric only over `UnitFamily`.

### Algorithm shape

Single bottom-up `checkExpr` for expressions; declaration-level checks (slot families, routine call arity, schedule label resolution, goal/direction consistency) are flat methods called from the entry `check(AthleteBlock)`. No exceptions thrown — diagnostics are collected into a `List<String>` and the caller (Main) decides whether to fail. This matches §4.5.8's "single linear pass" promise.

### Binary operator dispatch

The `applyBinary` switch implements §4.5.4–§4.5.5 case-by-case:

```java
case "+", "-":  Number+Number, Quantity D + Quantity D     // Rate+Rate rejected (§4.5.5)
case "*":      Number×Number, Number×Q, Q×Number, Rate(D,T)×Q(T), Q(T)×Rate(D,T)
case "/":      Number/Number, Quantity D / Number          // same-family Q/Q rejected
```

Rejected pairs return `null`; the caller emits a generic "operator '⊕' is not defined on T1 and T2 (§4.5.4–§4.5.5)" diagnostic.

### When-decl scoping (a design call Claude made on its own)

§4.4.10 says `<when-decl>` is statically dispatched and the surviving branch is "spliced" into the rules section. Claude's implementation type-checks **both** branches against a snapshot of the env (rather than picking one and skipping the other), and let-bindings introduced inside a branch are scoped to that branch only — they do not leak into the rules after the when. This is a defensible reading of "both branches must be well-typed even if only one runs" (Java treats `if (false) { ... }` the same way), but it is more conservative than the splice-and-evaluate reading the §4.4.10 text suggests. Worth confirming for the quiz; flagged in `[[feedback_fitlang_design]]`-adjacent territory.

## Tests run (known-answer inputs)

| Program | Expected | Actual |
|---|---|---|
| `samples/valid_01_full.fl` | well-typed | **type error caught**: bulk+lose inconsistency (this is a real bug in the sample, not a checker bug — already noted in entry 07's "errors I caught" section; the sample was never fixed) |
| `samples/valid_02_workout_only.fl` | well-typed | OK ✓ |
| `samples/valid_03_diet_only.fl` | well-typed | OK ✓ |
| `samples/_p2_expr_smoke.fl` (new, exercises §4.7 layer: `*`, `/`, `()`, unary `-`, rate atoms, Rate × Time) | well-typed | OK ✓ |
| `samples/_typeerr_cross_family.fl` (4 deliberate errors) | 4 rejections | 4/4 caught: `Mass + Energy`, `Mass / Mass`, `Mass * Mass`, unbound identifier ✓ |
| `samples/_typeerr_rate_and_slots.fl` (6 deliberate errors) | 6 rejections | 6/6 caught: exercise weight is Energy, progress rate denominator wrong, macro is Energy, per-unit wrong, goal-rate is Rate(Mass,Mass), unknown meal label ✓ |

Total: **5 distinct programs**, well clear of the ≥3 minimum. Coverage spans every §4.5 rule plus the §4.4.9 mode/direction consistency rule.

## Accepted

- The full `TypeChecker.java` as Claude wrote it. It compiled clean, every rejection path fired on the first run, and the diagnostics were specific enough that no message needed rewording.
- The decision to **collect errors rather than throw** on the first failure. Lets D3's type-error program report multiple problems in one run — matches the handout's spirit (the lifter sees every problem with their plan, not just the first).
- The `--no-check` flag in `Main` for cases where I want to dump the AST of an intentionally ill-typed program for D3.
- The UTF-8 console fix in `Main.main` — `§` was rendering as `�` on the Windows console because `System.out` defaulted to cp1252.

## Rejected / modified

- **Sample file `valid_01_full.fl`**, not the checker. The bulk+lose pair was already a known issue from entry 07; the checker exposed it again as a real diagnostic, and I changed `goal lose 0.5 kg/week` to `goal gain 0.5 kg/week` to match the bulk header. (The alternative — changing the header to `cut` — would have been equally correct; I picked `gain` to align with the §4.4.11 worked example direction.)
- Nothing in the checker code itself was modified after Claude's first draft.

## Errors I caught

- **No bugs introduced by Claude in this session.** Five programs (three positive, two negative-with-ten-deliberate-errors-between-them) all returned the expected verdict on the first compile. This is a stronger result than the experiment design technically requires, and I want to flag two reasons before drawing conclusions:
  - The checker is **small** (301 lines) and **structurally simple** (one method per node kind, no inference beyond syntactic dispatch). The §4.5.8 pseudocode in D1 is essentially the algorithm spelled out — Claude was transcribing a spec, not synthesising one.
  - The test set is **directed at the rules I knew Claude implemented**. Errors of omission — rules in §4.5 that Claude *didn't* implement and that my test programs don't exercise — would not be caught by this test methodology. A more adversarial test set (fuzzing, or one written by someone who hadn't read the implementation) would be a stronger check.
- **Real bug the checker caught** in `samples/valid_01_full.fl`: bulk header + `lose` direction. This is the §4.4.9 consistency rule firing on a sample that had been in the repo since Part 1 with the inconsistency unfixed. The journal of entry 07 had already noted it; the type checker now enforces it.
- **One design call worth re-examining**: the when-decl branch-local let scoping (see §"When-decl scoping" above). Claude implemented this without checking with me first. The choice is defensible but not the only defensible reading of §4.4.10.

## Addendum (same session, after the rest of the entry was drafted)

I asked Claude to spell out the when-decl scoping question. After seeing both readings, I picked the **splice-and-leak** reading (option 2): pick the surviving branch via the athlete header `goal`, type-check only that branch, let its bindings extend the env for the rules that follow. This matches §4.4.10's literal "splice" wording. A consequence I am explicitly accepting: **type errors in the dead branch are not compile errors** — Sebesta §8.1 static-if behaviour, defendable on those grounds.

Claude rewrote `checkWhenDecl` to this reading; the file-level Javadoc was also updated. All three valid samples and the merged `samples/invalid_typeerror.fl` (10 deliberate type errors) still pass / fail as expected after the change.

Also merged the two `_typeerr_*` temp programs into `samples/invalid_typeerror.fl` with numbered comments mapping each error to the §4.5 / §4.4 rule it violates. This is the D3 type-error program.

## Reflection

Checker came out clean on first compile, but most of the hard thinking already happend in D1 so this was more translation than design. The one real design call (when-decl scoping) Claude made silenty without asking, I overode it later. Test set is directed so errors of omission would not show up here.
