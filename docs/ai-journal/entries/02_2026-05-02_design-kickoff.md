---
entry: 01 
date: 02-05-2026
phase: Design
ai_tool: Claude Opus 4.7 (claude-opus-4-7) via Claude Code CLI
---

# Entry 01 — Project kickoff: domain selection, language choice, FitLang §4.1

## Goal
Read the CSE 341 project handout and submission guide; pick a DSL domain that meets the originality criterion; pick an implementation language; lock down the §4.1 Language Overview (name, sample program, justification) for FitLang; and sketch the type-system rules far enough that §4.2 (lexical structure) can be drafted next.

## Prompt
This was a multi-turn design conversation rather than a single prompt. The opening prompt was:

> *"Ok, we will start working on this project. Let's understand the requirements under docs folder and set up a folder for my AI journal. Btw I setup a github repo and I am currently in it so we will use my github account to store the source code with all docs and AI journal."*

Subsequent turns iterated through: (a) confirming solo / pair status and student number, (b) choosing the DSL domain — I rejected combining workouts with personal finance as a reskin risk and steered toward fitness + nutrition because the two share a real spine (typed physical quantities tracked over time against a body goal), (c) choosing Java over Python and C++, (d) committing to hand-written recursive descent over ANTLR, (e) iterative refinement of the type-system rules for unit arithmetic.

## Response (key part)

The AI proposed:
- **Domain:** `FuelLift`/`FitLang` — fitness training + nutrition, sharing one type system for `Mass`, `Energy`, macro grams.
- **Sample program** with `athlete { ... }` block, `workout`/`exercise`/`meal` blocks, `progress` rule, `target`/`goal` clauses, and `plan week with ...`.
- **Type-system rules**: dimensional unit coercion within a family (kg ↔ lb), type error across families (kg + m), with iterative refinement on what arithmetic is allowed.
- **Operation table** that was tightened across three rounds — initial draft allowed dimensionless results from `kg/kg`; final version bans same-unit multiplication and division entirely, allows scalar scaling, and adds a single "rate cancellation" rule (`Q<B> * R<A,B> → Q<A>`) so manual calorie computation works.
- **Variable declarations** with the unit as the type (`kg counter = 0`).
- **§4.1 Language Overview** drafted and written to `docs/design/D1.md`.

## Accepted
- The fitness+nutrition framing — coherent domain spine, not a reskin.
- Java + plain `javac` + hand-written recursive descent (matches Sebesta §4.4 directly; means every parser line is mine and defensible in the exam).
- The locked §4.1 sample program (athlete block, workout, meal, target, goal, plan).
- `80 kg` space-separated unit literals (cheapest to lex; units become reserved words).
- Two surface syntaxes for rates — `kg/week` and `per week` — both desugar to one AST node.
- Block-with-schema field syntax (`protein: 40 g`) distinct from typed variable declarations (`kg counter = 0`).
- Memorising user preferences: no globals, every keyword must carry meaning, every design option must cite a real language that uses it, technical depth assumed.

## Rejected / modified
- **Original `athlete { ... }` as a top-level singleton was rejected** — user pointed out (and Prof. Yusuf agrees) that globals are a code smell (Sebesta §5.5.5 backs this). Redesigned as a lexically scoped block; bodyweight resolves by static-scope lookup into the enclosing `athlete` block.
- **Initial filler keyword `by`** in `progress weight by 2.5 kg every week` was rejected — user demanded that every keyword carry semantic weight. Replaced with rate literal `2.5 kg/week`.
- **Initial unit-arithmetic table allowed `kg/kg → scalar`** — user flagged that dimensionless results are a leak. Tightened to: same-unit `*` and `/` are static type errors; if the user really wants a ratio they must write an explicit cast.
- **Strict ban on cross-dim arithmetic** initially blocked legitimate domain math like grams-to-kcal. Modified by adding **one** rule: rate cancellation (`Q<B> * R<A,B> → Q<A>`). Lets `meal.protein * 4 kcal/g → kcal` work without re-opening the door to `kg * kg`.
- **Strict `kg + int` rejection** loosened to literal-promotion only (Haskell-style polymorphic numeric literals). Then extended further when the user asked for typed-variable declarations (`kg counter = 0; counter += 5 kg`).
- **Initial Python recommendation overridden** — user chose Java.
- **Combining workouts with personal finance was rejected by me** before the user committed; user agreed and pivoted to nutrition instead.

## Errors I caught
1. **The AI initially placed `bodyweight` as a top-level global** in the sample program. I caught this and pointed out that we don't want globals. The AI then rebuilt the design around a scoped `athlete { ... }` block — which is materially better and now consistent with Sebesta §5.5 (static scoping rules). If I hadn't caught it, the §4.6 (Names/Binding/Scope) discussion would have had to defend an unnecessary global, which the rubric explicitly penalises.
2. **The AI's first type-rule table allowed `kg/kg → scalar`** (a raw `Float`). I flagged this as a leak — once units can be silently stripped, the reliability story collapses. The AI then proposed three increasingly restrictive options; I picked the strictest reasonable one.
3. **The AI's unit-arithmetic restrictions were too strict in the other direction** — it forbade the cross-dim multiplication needed to compute kcal from grams of protein, which would have made the domain half-unusable. Caught when I asked "how can we allow manual calculations?" — the AI then added the rate-cancellation rule.
4. **The AI used the filler word `by`** in a domain construct. I rejected it as semantically empty. The rule that *every keyword must mean something* is now saved in the AI's persistent feedback memory so the same mistake should not recur.

## Reflection
I went through the design phase with the AI and I discuced with the AI the options it gave me and the tradeoffs of these options and asked for recomendations alongside with review on my suggestions and preferences such as no globals rule or no dimensionless units (those resulted of oprations like kg/kg) and more that the AI was thinking in another direction or in different way. The guide I prepared for the AI for better book references was a good move and it made aligning the overall outputs with Sebasta's book better. Most of the time I found the need of asking clarifications on the given options and discussing and giving other options to do. Mostly I didn't accept anything as is. 
