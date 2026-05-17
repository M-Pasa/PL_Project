---
entry: 08
date: 2026-05-17
phase: Design — E2 experiment (name vs structural equivalence for FitLang records)
ai_tool: Claude Opus 4.7, Gemini 3.1 Pro, GPT 5.5 (three-model comparison)
---

# Entry 08 — E2: name vs structural equivalence, three-model comparison

## Goal

Run the mandatory E2 experiment: have an AI (with no prior FitLang context) explain whether FitLang's four record kinds should use name or structural equivalence in the Sebesta §6.14 sense, then compare the AI's reasoning against my §4.5.3 decision and against Sebesta directly. To get more signal than a single model, I ran the same prompt against three flagship models.

## Prompt

Same prompt to all three, fresh context (no D1, no §4.5.3 leak):

```
I'm designing a small DSL called FitLang for writing fitness training programs
together with nutrition. It has four record-like declarations, each introduced
by a fixed keyword:

  - exercise "L" { sets: N, reps: N, weight: Mass }
  - meal "L" { protein: Mass, carbs: Mass, fat: Mass } (one or more of these fields)
  - workout "L" { ... list of exercises ..., optional progress rate }
  - routine "L" (p: UNIT, ...) { ... list of exercises ... }

The grammar admits exactly one schema per kind keyword — a user cannot define
their own record kinds and cannot add fields. Mass is a dimensional quantity
type (kg, g, lb — all the same type).

Question: should FitLang use name equivalence or structural equivalence for
these record kinds? Walk me through both options in the Sebesta Concepts of
Programming Languages sense (Ch. 6), what each buys and costs, which real
languages take each path, and which you'd pick for FitLang. Be specific about
how the choice affects the type checker.
```

Raw outputs: `docs/ai-journal/experiments/E2/{claude_opus_4.7,gemini_3.1_pro,gpt5.5}.md`.

## Response — three-model comparison

### Verdict

All three models pick **name equivalence**. Unanimous, same direction as my §4.5.3.

### Defences they share

- **Closed universe makes structural pointless** — no user-defined records, so the structural-flexibility win has no surface to land on.
- **Semantic clarity** — `exercise` and `meal` are different domain concepts even if fields happen to overlap; nominal preserves that.
- **Simpler checker** — tag/kind equality is O(1); structural is a recursive walk over fields. All three mention this.
- **Better error messages** — "expected `exercise`, got `meal`" beats a field-shape error. All three quote this almost verbatim.

### Real-language citations (consistent with Sebesta §6.14)

| Equivalence | Languages named (union across all three models) |
|---|---|
| Name | Ada, Pascal, C/C++ structs, Java, C#, Rust, Swift, Haskell `newtype` |
| Structural | TypeScript, Go interfaces, OCaml, Elm |

No model invented a wrong citation. The lists overlap heavily and match Sebesta §6.14's own examples.

### Where each model added something distinct

- **Claude Opus 4.7** noted the `Mass` caveat: "the `Mass` type already does the right structural work at the field level (kg/g/lb), so structural equivalence isn't needed to handle the one genuinely structural thing in the language." Useful framing — separates the §4.5.3 record-kind question from the §4.5.4 within-family coercion question, which are easy to conflate.
- **Gemini 3.1 Pro** raised a future-proofing example: if `meal` shrinks to a single field and a new `cardio` kind gains the same single field, structural would silently mix them. Correct *if* the universe ever opens — but moot under §4.5.1's closed-universe commitment. Useful to keep on file in case the universe is later opened.
- **GPT 5.5** proposed a "hybrid: nominal at type-equivalence, structural as parse-time metadata." This is not a new idea — it is exactly what I am doing already (the parser enforces the fixed schema; the checker only ever compares kind tags). Worth citing as convergence with my implementation plan, not as a separate proposal.

## What none of the three noticed

The strongest defence of name equivalence in this language is **not** the domain-semantic one (which is all three models gave). It is the **grammar-collapse** argument my §4.5.3 makes:

> Because the grammar admits exactly one schema per kind keyword, name equivalence and structural equivalence accept and reject *the same set of legal FitLang programs*. The choice is therefore "which rule to teach", not "which programs to admit", and the one-sentence nominal rule ("same kind ⇒ same type") is the simpler teach.

None of the three models flagged this. They all argued nominal vs structural as if there were programs the choice would discriminate — there aren't, given the closed grammar. The grammar-collapse argument is the one that uses Sebesta's framework most precisely (§6.14 contrasts the two by which programs they admit; here the contrast vanishes), and the LLMs missed it across the board.

## Accepted

- **§4.5.3 stands unchanged.** Three flagship models converge on the same answer; the experiment is corroborating evidence, not a reason to revise.
- **Claude's `Mass`-is-the-structural-piece framing** — borrowed into my mental quiz defence: "the only structural typing in FitLang is at the unit/family level inside `Mass`, not at the record-kind level."
- **GPT 5.5's hybrid description** — noted as convergent with my parser-enforces-schema + checker-uses-kind-tags implementation plan.

## Rejected / modified

- **Gemini's future-proofing example.** Logically sound but predicated on the universe being open in a future version. Under §4.5.1 the universe is closed, so the example does not currently fire. Not a reason to revise §4.5.3 today; flagged for the §4.8 rationale paragraph on "closed universe" if a future revision opens it.
- Nothing else. The three models gave no proposal that contradicted §4.5.3.

## Errors I caught

- **No factual errors in any of the three outputs.** All language citations match Sebesta §6.14; no hallucinated APIs; no wrong claims about Ada / Pascal / TypeScript / Go.
- The **omission** of the grammar-collapse argument across all three models is not an "error" in the strict sense (the prompt did not require it), but it is the most informative finding of the experiment: a unanimous AI verdict that misses the strongest leg of the defence I had already written into §4.5.3. The quiz risk this surfaces is real — if I had drafted §4.5.3 from an LLM rather than first-principles, I would not have the grammar-collapse line in my defence, and that is the line a §6.14-literate examiner would want to hear.

## Reflection

Three models gave same answer, all missed the grammar-collapse argument which is the actual strongest defence. They aggreed with each other becasue they all read the same textbook, not because they tought independtly.