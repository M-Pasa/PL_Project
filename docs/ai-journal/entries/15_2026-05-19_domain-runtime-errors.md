---
entry: 15
date: 2026-05-19
phase: Interpreter | Spec | Sample programs
ai_tool: Claude Opus 4.7
---

# Entry 15 — Domain-specific runtime errors (handout §D2 P2 requirement)

## Goal

Close the last open handout requirement. §D2 P2 lists three required
runtime diagnostics: division by zero, index out of range (if applicable),
and "at least one domain-specific error". FitLang had only division by zero
in four arithmetic shapes; the domain-specific error was missing. Caught
this in a pre-submission re-read of the handout, not in a graded artifact.

## Prompt

After confirming the implementation covers every D1 feature (entry 14 +
the `valid_06_comprehensive.fl` + `valid_07_cut_branch.fl` verification
pass), I asked which handout requirements were still open. The
handout-vs-implementation grep turned up "domain-specific error" as the
one bullet without a corresponding diagnostic in the interpreter. We
considered three candidates: (1) negative computed Mass at a slot,
(2) negative computed macro Mass at a meal, (3) projected bodyweight
reaching ≤ 0 under a `lose`-direction multi-week plan, and (4) an
end-of-horizon composite that subsumed both. The decision was to
implement (1)+(2) as one helper called from two slots (Plan A) AND (3)
as a separate check in the multi-week unfolder (Plan B), because they
fire on different kinds of plans — A on slot-level subtraction, B on
horizon × rate × starting-weight composition.

## Response (key part)

`Interpreter.requireNonNegativeMass(MassV, where)` called from
`evalExercise` (after the base + progression offset is computed, so
progression-driven negatives also fire) and from `evalMeal`'s macro
loop. Diagnostic shape:

```
Runtime error: exercise "Bench Press" weight evaluated to -10 kg at week 1 (must be ≥ 0)
Runtime error: meal "X" macro protein evaluated to -10 g at week 1 (must be ≥ 0)
```

For bodyweight projection, a `<= 0.0` check at the top of the §4.4.8
loop, after the projected bodyweight is computed but before that week's
workouts/meals are evaluated. The diagnostic carries the responsible
variables (direction, rate, horizon, starting weight, offending week):

```
Runtime error: bodyweight projected to 0 kg at week 16 (must be > 0);
goal lose at 2 kg/week over plan 20 week with starting bodyweight 30 kg
```

`Main.java` had a single `catch (FitLangException)` that prefixed every
diagnostic with "Parse error:" — wrong now that the same exception
carries runtime errors too. Split into two catches: the parse-time one
keeps the prefix, the run-time one uses "Runtime error:".
`FitLangException` also stopped suffixing `[line 0, col 0]` when the
exception is built with line=0 (runtime errors have no source
coordinates).

D1 grew a §4.4.12 documenting the two invariants (R1, R2) as
runtime-checked, with the Sebesta §6.3/§6.4 Ada-style runtime-range
analogue cited for R1 and the "would need dependent typing" trade-off
noted for R2. D3 grew a §4 with both samples and their diagnostics.

## Accepted

- **Both checks per-week, not end-of-horizon.** Plan C in my pre-impl
  notes (only error at week N) would have been observationally
  equivalent for FitLang's grammar (no construct can drive a slot
  back up after going down), but Plan A (per-week) is cheaper to
  implement and produces a more precise diagnostic — "week 16" beats
  "the final week was negative, somewhere along the way it went
  through zero".
- **R1's bound is `< 0`, R2's bound is `<= 0`.** Zero weight on an
  exercise is borderline meaningful (a deload week could conceivably
  say zero) but zero bodyweight is not. Different tolerances per
  invariant. Documented in §4.4.12.
- **`Main.java` catches split parse-time and run-time.** A single
  catch with a runtime check on the cause was considered; the split
  is simpler, the two phases never interleave, and the
  reader-of-Main sees exactly where each kind of error originates.

## Rejected / modified

- **Did not add a `--no-runtime-checks` flag.** Considered briefly to
  make the runtime checks toggleable for performance benchmarking.
  Rejected because there is no production performance pressure on
  a course interpreter, and two `if (x < 0)` comparisons per slot
  per week are vanishingly cheap. Trading runtime safety for ~0%
  speedup is the wrong direction.
- **Did not embed (R2) as a sign check on the unfolder loop bound.**
  Could have computed `kMax = floor(bw₀ / rate · week)` and clamped
  N to that, but clamping silently truncates the user's `plan N week`
  — they asked for N weeks and would not see the truncation in any
  error. Aborting at the offending week and naming it in the
  diagnostic is the right behaviour.

## Errors I caught

- **First version of R1 fired only on the pre-progression base
  weight.** `evalMass(e.weight).g` was checked but the
  post-progression `base_g + progressGPerSec * offsetSec` was not.
  A plan that starts at 1 kg and progresses at `-2.5 kg/week` would
  pass week 1 (1 kg ≥ 0) and silently produce -1.5 kg at week 2.
  Moved the check to *after* the progression offset is added.
- **`Main.java` "Parse error: division by zero" was a pre-existing
  mislabel.** Division by zero in the interpreter has been a
  runtime fault since entry 11 but `Main` was prefixing it with
  "Parse error:" because the exception class was shared. Fixing the
  split also fixed that long-standing wrong label. Not my work
  originally — but the split commit corrects it.

## Reflection

I rechecked the handout one more time before calling the submission done and I found that we don't do much checks on runtime other than division by 0. We introduced further checks such as negative weights after expression evaluation and unrealistic progress such as 2 kg for 40 weeks where the body weight is like 70 which will result in negative result. The AI missed that but luckily I caught it.