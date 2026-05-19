---
entry: 14
date: 2026-05-19
phase: Type checker | Sample programs
ai_tool: Claude Opus 4.7
---

# Entry 14 — Routine-parameter binding in the type checker

## Goal

Close a real spec/implementation gap I caught while writing the D5 P2
self-assessment. The interpreter has substituted routine parameters at the
call site since entry 11 (`Interpreter.evalWorkout` lines 250-260: eval
args in caller env, push into env, walk body, restore). The type checker
did *not* match: `checkRoutineDecl` had a comment "no routine-parameter
binding yet — that happens at the call site", which meant any routine body
that referenced its parameter by name (`weight: load`) was a "unbound
identifier 'load'" error at compile time. `valid_02_workout_only.fl`
worked around this by having the routine body use literals (`weight: 80
kg`), making the typed parameters look declarative-only.

## Prompt

After writing the D5 retrospective I had a "what didn't work" paragraph
that said the params are declared but not substituted. The user asked
whether substitution was hard to add. Re-reading the interpreter showed
substitution was already there; the only piece missing was the type
checker's env push.

## Response (key part)

One small edit in `TypeChecker.checkRoutineDecl`:

```java
Map<String, FType> saved = new LinkedHashMap<>(env);
for (Ast.RoutineParam p : r.params) env.put(p.name, new TQuantity(p.family));
for (Ast.ExerciseDecl e : r.exercises) checkExerciseDecl(e, ...);
if (r.progress != null) checkProgressRate(r.progress.rate, ...);
env.clear(); env.putAll(saved);
```

The pattern is the same save/restore the interpreter uses (`savedEnv`
followed by `clear() + putAll`). The push uses the *declared* family
(`TQuantity(p.family)`) — the call-site check in `checkRoutineCall`
already enforces that the call-site argument matches the same family,
so the body and the call site agree on the type by construction.

Then `samples/valid_02_workout_only.fl` got its routine body changed to
reference the params:

```
routine "Base Press"(load: kg, accessory_load: kg) {
    exercise "Bench Press"    { sets: 4, reps: 6, weight: load }
    exercise "Overhead Press" { sets: 3, reps: 8, weight: accessory_load }
}
```

`--run` now shows `Bench Press @ 82 kg` and `Overhead Press @ 22 kg`
inside Push Day — substitution of the call-site args `(82 kg, 22 kg)`.
D3 and D5 P2 updated to match: D3 valid_02 source/dump/run/discussion
all rewritten, D5 lost its "scoped out" paragraph on this gap and
Correctness moved B → A.

## Accepted

- **Push params with declared family, not the call-site arg type.** The
  call-site arg is checked against `decl.params.get(i).family` in
  `checkRoutineCall` independently. Pushing the *declared* family keeps
  the routine body's type discipline single-source: whatever the body
  expects of `load`, every call site has to provide. Pushing the
  call-site arg type would let one call site widen the body's typing
  past what another call site can support.
- **Save/restore via LinkedHashMap copy.** Same idiom the interpreter
  uses. Two routines never coexist in the env (each is processed
  sequentially), but the save/restore makes the function self-contained
  and survives future refactors that might check routines from a
  non-empty env.

## Rejected / modified

- **Did not promote routine params to "always Quantity(MASS)".** The
  `RoutineParam` AST already carries a `UnitFamily` — the parser reads
  `load: kg` and tags `MASS`. The type checker should respect what the
  declaration says, not over-specialise. (None of the existing samples
  declare a non-Mass routine param, but the type system supports it
  by construction once the env push uses the declared family.)
- **Did not add a `<routine-param>` slot that defaults to a literal.**
  Considered briefly to make the existing sample "still work" without
  the substitution. Rejected — the simpler change is to use the params
  the declaration already promises.

## Errors I caught

- **My own D5 retrospective wording was wrong.** I had written "params
  declared but not substituted, because the substitution requires a
  deep AST rewrite per call site or a runtime substitution pass". The
  runtime pass already existed; what was missing was three lines in
  the type checker. Re-reading the interpreter before writing the
  retrospective would have caught this — a lesson on grounding
  retrospectives in code, not in memory of "I think I didn't do that".

## Reflection (2–3 sentences)

For the D2 grade, we have to make sure our type checker and interpreter give the same results. I just realized mine had been out of sync since entry 11. Luckily, it was an easy fix. The type checker was mostly set up right, but it wasn't keeping track of the variable types inside function bodies. The interpreter was already doing this correctly when it ran the code, so I just had to update the type checker to match.