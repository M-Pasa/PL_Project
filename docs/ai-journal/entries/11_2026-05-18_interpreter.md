---
entry: 11
date: 2026-05-18
phase: Interpreter
ai_tool: Claude Opus 4.7
---

# Entry 11 — Tree-walking interpreter for P2 §4.4

## Goal

Implement the §4.4 interpreter (`Interpreter.java`) on top of the existing
lexer/parser/type checker, ending Part 2's D2 code with a working
`fitlang … --run` that prints a `Plan` for valid programs.

## Prompt

Two-step conversation: (1) "we were about to work on interpreter right?" →
asked for the design first; (2) after the design sketch + two clarifying
questions → "yes" to draft `Interpreter.java`.

## Response (key part)

Design sketch I worked from:

- Pipeline: Lexer → Parser → TypeChecker → Interpreter. Interpreter trusts
  the checker — no family/operator-algebra re-validation, only computation.
- Output denotation = `Plan { athlete, horizon, weeks: [Week_k] }` per §4.4.2.
- Value hierarchy: `NumV`, `MassV(grams)`, `EnergyV(kcal)`, `TimeV(seconds)`,
  `RateV(v, numerFam, denomFam)`. **Canonicalize at construction** (§4.4.3) so
  every arithmetic op is plain doubles. Display unit chosen per role at print
  time, not per family.
- Environment is a flat `Map<String, Value>` for the athlete scope, extended
  per `RoutineCall` and restored on return (no closures).
- `when` dispatched statically against `athlete.params.goal` and the surviving
  branch spliced into the rule stream — mirrors what `TypeChecker.checkWhenDecl`
  already does.
- Operator dispatch keyed on `(op, leftClass, rightClass)`; one case per
  §4.5.4–§4.5.5 rule. Unhandled combo = checker bug, not user input.
- Single runtime error class: divide-by-zero in `Num/Num` (and the Quantity/Num
  variants).

## Accepted

- The role-keyed display table that came out of the print-unit discussion (see
  Rejected below):
  - `AthleteParams.bodyweight`, `ExerciseDecl.weight` → **kg**
  - `MacroDecl.quantity` → **g**
  - `GoalRateDecl.rate`, `ProgressStmt.rate` → **kg/week**
  - Horizon → **week**
  - `TargetDecl` threshold → **echo declared unit** (plus canonical-g in parens
    when `per X of bodyweight` resolves to a different number)
  - Planned macro totals + `OFF by Δ` delta → **g** (measured-side role)
- Eager N-week materialisation. Loop structure is in place but degenerates to
  N=1 for now (see §4.4.2: "a program with `plan week …` (no integer)
  degenerates to N=1").
- Static `when`-splice helper (`spliceRules`) used **before** the per-week
  loop, so the live rule list is computed once and the loop reads from it.

## Rejected / modified

- **AI's first print-unit proposal: "family-default display unit (Mass→kg,
  Energy→kcal, Time→week)."** Rejected — Mass spans 4 orders of magnitude in
  this language (300 g protein vs 80 kg bodyweight), and Time spans 7 (1 week
  vs an exercise rest interval). One display unit per family is wrong by
  construction. The role-keyed table above is the replacement.
- **Global "display unit" setting (Mass=kg or Mass=g globally).** Rejected
  during the same exchange — it relocates the ambiguity instead of removing
  it. Either macros print as `0.04 kg` or bodyweight prints as `78000 g`;
  neither is defensible.
- **OFF-by delta echoing the threshold's declared unit.** I had it both ways
  for a moment in the design discussion; settled on the measured-side role
  (i.e. macro totals are in g, so deltas are in g) because the delta is a
  difference of two planned-side quantities, not a property of the threshold.
- **`sealed interface Value permits …` (Java 17 records).** Reverted to plain
  abstract class + `instanceof` checks to match the rest of the codebase
  (`TypeChecker.FType` uses the same pre-pattern-matching style).

## Errors I caught

- **Spec/implementation gap on `plan N week`.** §4.4.8 talks about N-week
  projection, but the current `Parser` emits `plan week with …` (no `N`) and
  `ScheduleStmt` has no week-count field. The AI's first draft would have
  read `schedule.N` and crashed; I caught this by reading `Parser.java`
  before writing, and committed to the §4.4.2 N=1 degenerate case explicitly
  in the doc comment. The structural loop is in place so the future
  parser/AST extension is purely additive.
- **`let` bindings aren't referenced anywhere in P1.** Workout slots take
  `<quantity>`, not `<expression>`, so `IdentExpr` never appears in any
  executable position. The interpreter still evaluates them into the env —
  cheap, and means §4.7's eventual widening of those slots is a one-line
  change. Without this check I'd have had a dead-code section.
- **Routine-call env restoration.** First sketch left the parameter bindings
  in the env after the call returned, which would have leaked routine
  parameters into later workouts of the same schedule. Fixed by snapshotting
  the env before binding params and restoring it on the way out — the
  single-frame stand-in for the "no closures" decision.
- **Division by zero is the only runtime error class.** Verified by reading
  `TypeChecker.applyBinary`: every other operator combination the checker
  admits has a total interpretation. Easy to miss; would have left me adding
  defensive checks that the type system already guarantees redundant.

## Reflection (2–3 sentences)

The agent wanted to make unit displays as default which may cause some weird displays like 78000 g for body weight, we handled that by categorizing the
outputs for example for bodyweight kg, for macros g is used and so on. Also noticed `plan N week` from §4.4.8 isn't actually in the parser yet — for now
the interpreter runs the N=1 case from §4.4.2, and adding multi-week is the next parser/AST task.