---
entry: 06
date: 08-05-2026
phase: D4 Experiment E1 — cross-model EBNF comparison
ai_tool: Claude Opus 4.7, GPT-5.5, Gemini 3.1 Pro
---

# Entry 06 — E1: AI-generated EBNF vs. §4.3 grammar (3 models)

## Goal

Run experiment E1 from D4: hand the same paragraph description of FitLang to three frontier LLMs (one shot, no design context beyond the paragraph), collect their EBNF, and compare each output against the LL(1) grammar in D1 §4.3 to see (a) which structural decisions converge across models, (b) which diverge, and (c) what each one gets wrong relative to the spec.

Raw outputs are stored in `docs/ai-journal/experiments/E1/{opus4.7,gpt5.5,gemini3.1-pro}.md`.

## Prompt

Identical single user turn given to all three models, no system message, no follow-up:

> FitLang is a domain-specific language for writing personal fitness programs that combine strength training and nutrition planning. Every FitLang program describes a single athlete, identified by their bodyweight (a physical quantity with a mass unit like kg or lb) and a goal mode (bulk, cut, or maintain). Inside the athlete context, the user can define one or more named workout blocks, each containing one or more named exercises. Each exercise specifies a number of sets, a number of reps, and a load weight. A workout may optionally include a progression rule that states how much the weight should increase per unit of time (for example, 2.5 kg per week). The user can also define one or more named meal blocks, each listing macronutrient amounts — protein, carbs, and fat — as physical quantities in grams. After the workout and meal definitions, the user may write rules: a target rule constrains a macro to be above or below a threshold (optionally scaled per kilogram of bodyweight), a goal rule states a desired weight-change rate, and a let binding gives a name to a numeric or quantity value for reuse. Every program ends with a schedule statement that names which workouts and meals to include in a planned week. Physical quantities always pair a number with a unit; units belong to families (mass, energy, time) and mixing families is an error.
>
> I want you to write EBNF grammer rules for this language.

## Response (key parts, verbatim excerpts)

### Claude Opus 4.7 (`opus4.7.md`)

```
athlete_block   = "athlete" identifier "{"
                    bodyweight_decl
                    goal_decl
                    { workout_block }
                    { meal_block }
                    { rule }
                    schedule_stmt
                  "}" ;
exercise        = "exercise" identifier "{"
                    "sets"   "=" integer  ";"
                    "reps"   "=" integer  ";"
                    "load"   "=" quantity ";"
                  "}" ;
threshold       = quantity [ "per" "kg" "bodyweight" ]
                | number   [ "per" "kg" "bodyweight" ] ;
goal_rule       = "goal_rate" "=" signed_quantity "per" time_unit ";" ;
time_unit       = "sec" | "min" | "hour" | "day" | "week" | "month" ;
```

Notes from the model: it explicitly observed that unit-family safety is **not expressible in EBNF** and belongs in semantic analysis, and used `signed_quantity` for `goal_rate` so cutting/bulking can both be expressed.

### Gemini 3.1 Pro (`gemini3.1-pro.md`)

```
AthleteBody     ::= BodyweightDecl GoalModeDecl WorkoutDecl+ MealDecl+ RuleDecl* ScheduleDecl
WorkoutDecl     ::= "workout" Identifier "{" ExerciseDecl+ ProgressionRule? "}"
ExerciseDecl    ::= "exercise" Identifier "{" "sets" Number "reps" Number "load" Quantity "}"
ProgressionRule ::= "progression" Quantity "per" TimeUnit
MealDecl        ::= "meal" Identifier "{" "protein" Quantity "carbs" Quantity "fat" Quantity "}"
TargetRule      ::= "target" Macro Operator Value [ "per" "kg" ]
GoalRule        ::= "goal_rate" Quantity "per" TimeUnit
TimeUnit        ::= "week" | "day" | "month"
```

The model forced **`WorkoutDecl+ MealDecl+`** — both at least one — instead of allowing diet-only or workout-only programs.

### GPT-5.5 (`gpt5.5.md`)

```
program =
    athlete_decl,
    "{",
        { workout_block | meal_block | rule },
        schedule_stmt,
    "}" ;
athlete_decl =
    "athlete", quantity, "goal", goal_mode ;
exercise_decl =
    "exercise", identifier, "{",
        "sets", integer, ";",
        "reps", integer, ";",
        "load", quantity, ";",
    "}" ;
comparison_op =
    "above" | "below" | ">=" | "<=" | ">" | "<" ;
goal_rule =
    "goal", "weight_change", quantity, "per", time_unit, ";" ;
```

GPT-5.5 used a **free-order body** (`{ workout_block | meal_block | rule }`), invented English keywords `above`/`below` not in the spec, and renamed the goal-rate construct to `goal weight_change`.

## Comparison vs. §4.3 (technical, mechanical)

| §4.3 design choice                                         | Opus 4.7              | Gemini 3.1 Pro                    | GPT-5.5                          |
|------------------------------------------------------------|-----------------------|-----------------------------------|----------------------------------|
| Athlete header `athlete(bodyweight: Q, goal: M)`           | Different — separate `bodyweight_decl` / `goal_decl` inside body, with `=` | Different — `BodyweightDecl GoalModeDecl` as first body items | Different — flat positional `athlete quantity "goal" goal_mode`, no parens |
| `workout` body must have ≥1 `exercise`                     | ✅ `exercise { exercise }` | ✅ `ExerciseDecl+`                | ❌ Free-order body, no `+`       |
| Workout-only or meal-only programs allowed                 | ✅ `{ workout } { meal }` (both `*`) | ❌ `WorkoutDecl+ MealDecl+` requires both | ✅ free-order, but no `+` enforcement at all |
| Mandatory `<schedule-stmt>` at end                         | ✅ `schedule_stmt` last | ✅ `ScheduleDecl` last            | ✅ `schedule_stmt` after body    |
| `<rate>` as a 3-token construct `NUM UNIT "/" UNIT`        | ❌ uses `quantity per time_unit` | ❌ uses `Quantity "per" TimeUnit` | ❌ uses `quantity "per" time_unit` |
| Closed lexicon — string literals for names, no IDENT       | ❌ uses `identifier` everywhere | ❌ uses `Identifier` everywhere | ❌ uses `identifier` everywhere |
| Reserved-word `week` doubles as time unit                  | ❌ `time_unit` includes `month`, `hour`, `sec` not in spec | ❌ `TimeUnit` adds `month`        | ❌ adds `month`                  |
| Goal mode set is exactly `{bulk, cut, maintain}`           | ✅                    | ✅                                | ✅                               |
| Macro decls inside `meal` are `+` (one-or-more)            | ❌ exactly three `macro_decl` | ❌ exactly three (positional)    | ❌ exactly three (positional)    |
| `target ... per UNIT of bodyweight` — generic UNIT, `bodyweight` keyword | ❌ hardcoded `"per" "kg" "bodyweight"` | ❌ `[ "per" "kg" ]`, drops `bodyweight` | ❌ `per_bodyweight_expr` only allows `kg` |
| Relational ops `>= <= > < == !=`                            | ❌ missing `==` and `!=` | ❌ missing `!=`                  | ❌ adds `above`/`below`, missing `==`/`!=` |
| `<expression>` only supports additive `+ -` in P1          | n/a — no expression nonterminal | n/a — no expression nonterminal | n/a — no expression nonterminal |
| `<body-sections>` two-alternative production (plan / intake) | ❌ not modelled       | ❌ both required                  | ❌ free-order                    |

## Errors caught (per model)

**Opus 4.7**
- Adds `oz`, `month`, `hour`, `sec` to the unit lexicon — none appear in §4.2.6.
- Inserts `;` statement terminators that FitLang does not have.
- Drops the dual role of `week` as both a reserved word and a time unit.
- Uses `identifier` for athlete/workout/meal/exercise names instead of `STRING_LIT`.

**Gemini 3.1 Pro**
- Forces `WorkoutDecl+ MealDecl+`, eliminating workout-only and diet-only programs which the spec explicitly allows.
- Drops the `bodyweight` keyword from the per-bodyweight target form.
- Does not model the per-bodyweight scaling carries `UNIT of bodyweight` with arbitrary mass unit.
- Adds `cal` and `month` to the unit lexicon; misses `kJ`, `s`, `min`, `h`.

**GPT-5.5**
- Invents `above` / `below` as comparison operators; spec only defines symbolic ones.
- Renames the goal-rate construct to `goal "weight_change" quantity "per" time_unit`, replacing the rate token altogether.
- Uses `{ workout_block | meal_block | rule }` — fully free order — losing the §4.3 structural ordering (workouts before meals; rules in their own section; schedule last).
- Drops the parenthesised athlete header entirely.

## Convergence vs. divergence

Convergent (all three matched the spec or each other):
- All three reduced `program` to a single athlete block.
- All three split units into mass / energy / time families.
- All three flagged the unit-family check as semantic, not syntactic — none tried to encode it in EBNF.
- All three produced the goal mode set `{bulk, cut, maintain}`.

Divergent (each model picked something different):
- Body ordering: strict (Opus, Gemini) vs. free (GPT-5.5).
- Statement terminator: `;` (Opus, GPT-5.5) vs. none (Gemini).
- Goal-rate keyword: `goal_rate` (Opus, Gemini) vs. `goal weight_change` (GPT-5.5).
- Mandatory-meal: required (Gemini) vs. optional (Opus, GPT-5.5).

The biggest structural divergence from §4.3 that **all three** got wrong identically: none modelled `<rate>` as a primitive 3-token construct (`NUM UNIT "/" UNIT`); all three composed it as `quantity "per" time_unit`. This is interesting because it suggests the paragraph description does not communicate the `kg/week` slash-syntax clearly enough — a real spec ambiguity, not a model error.

## Accepted

AI design choices that match (or are compatible with) §4.3 — I'm keeping them in my own grammar and the cross-model agreement reinforces the call:

- **Single athlete block as the program root.** All three reduced `program → athlete_block`. Same as §4.3.1. Confirms the "one athlete per program" framing in the paragraph reads unambiguously.
- **Goal mode is exactly `bulk | cut | maintain`.** Closed three-element set in all three outputs and in §4.3.
- **Unit families (mass / energy / time) split into separate nonterminals.** All three did it; §4.3 does the same. Reading from these I'm more confident the family-disjoint design is the natural one.
- **Unit-family safety belongs in semantic analysis, not EBNF.** Opus 4.7 stated this explicitly; Gemini said it in commentary. I made the same decision in D1 §4.6 / future §4.5 (type checker). Useful triangulation — the AI evaluation matches Sebesta §6 territory.
- **Schedule statement at the end of the program.** Opus and Gemini placed it last; §4.3 makes it mandatory and final via `<athlete-block>`'s fixed tail. GPT-5.5 also put it last but allowed free-order body before it.

## Rejected / modified

AI choices I'm overriding — for each, the §4.3 decision and the reason it's the right call for FitLang specifically:

- **Statement terminator `;` (Opus 4.7, GPT-5.5).** §4.3 has no statement terminator — newlines and braces delimit. Reason: FitLang programs are read by lifters drafting a weekly plan, not by C programmers. `sets: 4, reps: 6, weight: 80 kg` reads as a row in a training log; adding `;` after every line is noise. The paragraph never mentioned `;` — both models inserted it from C-family priors.
- **`identifier` for athlete/workout/exercise/meal names (all three).** §4.3 uses `STRING_LIT` ("Bench Press", "Push Day"). Reason: workout and exercise names are *human-language phrases* with spaces and capitalisation, not C-style identifiers. The closed-lexicon constraint (D1 §4.2.5) means there are no user-introducible identifiers at all except inside `let` — every name in the language is either a reserved word, a unit, or a quoted string. None of the three AI outputs picked up the closed lexicon from the paragraph; that's a real signal that the paragraph under-specifies it.
- **`time_unit` adds `month`/`hour`/`sec`/etc. (Opus, Gemini, GPT-5.5).** §4.3 / §4.2.6 has exactly `s, min, h, day, week`. Reason: every other time unit is a fixed multiple of seconds; `month` is 28–31 days, breaking dimensional coercion within the TIME family. I keep the closed unit set.
- **`<rate>` as `quantity "per" time_unit` (all three).** §4.3 makes `<rate>` a 3-token primitive `NUM UNIT "/" UNIT`. Reason: lifters write "+2.5 kg/wk" in training logs, not "2.5 kg per week" — slash-syntax matches the writability target (Sebesta §1.3.2). However, since all three models converged on `per` instead, this is an *under-specified* part of my paragraph description; I'd add an example like `progress weight 2.5 kg/week` if I were rewriting the prompt. That's a prompt problem, not a model problem.
- **Forced `MealDecl+` (Gemini).** §4.3 allows workout-only and diet-only programs via the `<body-sections>` two-alternative production. Reason: a strength athlete in a maintenance phase often tracks workouts without macros, and a recovering injured athlete may track macros without lifting. Gemini's reading of the paragraph as "must contain meals" is too literal.
- **`above` / `below` as comparison operators (GPT-5.5).** §4.3 uses `>= <= > < == !=` only. Reason: English-keyword comparators conflict with the strict closed lexicon and add no expressive power.
- **Free-order body `{ workout | meal | rule }` (GPT-5.5).** §4.3 enforces workouts before meals before rules before schedule. Reason: the ordering carries semantic information — workouts and meals declare the *referents* that the rules and schedule then talk about. Allowing rules before workouts opens the door to forward references, which I don't want in P1.
- **Per-bodyweight scaling hardcoded to `kg` (all three).** §4.3 makes the unit generic: `per UNIT of bodyweight`, with `bodyweight` as a keyword. Reason: a US user may write `per lb of bodyweight`; the language already has `lb` in the mass family.

## Errors I caught

Beyond the design overrides above, here are the model errors that would have broken parsing or the lexicon if I'd accepted them:

1. **Opus 4.7 added `oz` to `mass_unit` and `sec`/`hour`/`month` to `time_unit`** — none appear in §4.2.6. Detected by direct lexicon comparison. If accepted, the lexer's closed unit table would have to be expanded, weakening the unit-family invariant.
2. **Gemini dropped the `bodyweight` keyword from the per-bodyweight target form** (`[ "per" "kg" ]` only). Detected by tracing the §4.3.2 production. If accepted, the rule would lose its readability — `target protein >= 1.8 g per kg` reads as "1.8 g per kg of *what*?".
3. **GPT-5.5 invented the keyword `weight_change`** in `goal "weight_change" quantity "per" time_unit`. Not in the spec, never mentioned in the paragraph. Detected on first read. Would have polluted the closed reserved-word table.
4. **All three missed the dual role of `week`** (reserved word *and* time unit). Detected because my lexer (`Lexer.java` line ~25) explicitly handles this with a single `WEEK` token. If accepted, the lexer would need a second `WEEK_UNIT` token type and a context rule to disambiguate — exactly the kind of ad-hoc escape from the closed lexicon I'm trying to avoid.

**On operator precedence (the handout question for E1):** none of the three modelled an `<expression>` nonterminal at all. The paragraph mentions `let` bindings to a "numeric or quantity value for reuse" but doesn't show arithmetic, so the models didn't invent one. §4.3 has `<expression> → <term> { ("+"|"-") <term> }` with only additive operators in P1 (multiplicative deferred to §4.7 in P2). So the operator-precedence question is moot here: there is no test of precedence in any of the three AI grammars. That itself is a finding — it means the paragraph fails to communicate that `let` bindings use a real expression sublanguage. Worth fixing in the P2 prompt.

## Reflection

The code structure from different AI models was similar, because the code style was discussed with Claude and we approved it since it has readable and clear structure. But the AI models tented to add thigns that are either useless or incorrect. Mostly becasue they don't have any context and they dollowed the same thinking style. Also other AI models missed some spec details like the closed lexicon, the dual role of week, and the slash-syntax for <rate> and none of the three caught all of them.