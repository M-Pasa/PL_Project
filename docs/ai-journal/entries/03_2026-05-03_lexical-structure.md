---
entry: 03
date: 03-05-2026
phase: Design
ai_tool: Claude Opus 4.7 (claude-opus-4-7) via Claude Code CLI
---

# Entry 03 — D1 §4.2 Lexical Structure

## Goal
Lock down FitLang's lexical structure for the D1 Part 1 submission: token classes, comment syntax, identifier rules, the unit-literal lexing strategy, and (the big one) whether FitLang has user-introduced identifiers at all.

## Prompt
Multi-turn working session. Opening prompt:

> *"yeah let's do the lexical structure"*

Subsequent turns negotiated five specific calls the AI surfaced as having "downstream consequences" before drafting: open vs closed lexicon, unit suffix as one token vs two, comment syntax, case sensitivity / Unicode, numeric literal forms.

## Response (key part)
The AI proposed five up-front decisions and asked for a yes/no on each. After my answers it produced an eight-subsection §4.2 covering: charset/whitespace, `#` line comments, the seven token classes with regex (RESERVED, MODE_LIT, UNIT, NUM_LIT, STRING_LIT, IDENT, PUNCT), string-literal escapes, the closed reserved/mode tables, three unit families with lexemes, the punctuation alphabet, and identifier rules. Sebesta cites tied to §1.3.1, §3.1, §4.2.

The most consequential proposal was the **closed-lexicon + numeric-`let`-only (option ii)** stance: every unquoted alphabetic lexeme is reserved or a unit; the only user-introduced names are string literals (`"Push Day"`) and scalar/quantity `let` bindings. The AI laid out three shapes — fully closed (i), closed+numeric let (ii), fully open (iii) — with a comparison table covering lexer/parser cost, §4.6 substance, type-system implications, DSL identity, expressive power, and Sebesta defenses, and recommended (ii).

## Accepted
- **Option (ii) closed lexicon + numeric `let`** — only after I asked whether (ii) → (iii) was a forward-compatible path. The AI's answer (additive grammar relaxation, type-system-only change later, no breaking redesign, with the small concrete advice "define one `value_expr` non-terminal in §4.3 now and type-restrict in §4.5") was specific enough to accept.
- **Option B for unit suffixes** — `NUM_LIT` and `UNIT` as two separate tokens, paired at parse time. Cheaper lexer, regular grammar, matches how C++ user-defined literals are actually scanned.
- `#` line comments only; no block comments.
- Case-sensitive identifiers; ASCII-only identifiers; Unicode allowed in string literals (so `"Kahvaltı"` is a legal label).
- Numeric literal regex `[0-9]+(\.[0-9]+)?` — no exponent, no hex/octal. AI's argument that the domain has no quantity that benefits from `1.5e3` was correct.
- The reserved word list (`athlete workout exercise meal progress target goal plan week with workouts meals let bodyweight sets reps weight protein carbs fat per of and`) and mode literals (`bulk cut maintain lose gain`) as separate token classes.
- The maximal-munch rule for multi-character punctuation and the explicit rejection of `===`.

## Rejected / modified
- **Macro as a separate unit family was rejected.** AI's draft had four families (Mass / Energy / Macro / Time) with `g` Macro-only — making `bodyweight: 70000 g` a type error. I pushed back: protein in grams *is* mass, the split was artificial and gratuitously strict. AI agreed (its own argument: "three families instead of four keeps the type rule simple") and collapsed to **Mass = {g, kg, lb}**. Within-family coercion now makes `70000 g` and `70 kg` interchangeable. This also simplifies §4.5.
- **No equality operator was rejected.** AI initially excluded `==` from the punctuation alphabet, arguing FitLang only needs `>=`/`<=` against thresholds. I overrode — `==` is needed for predicates (`if mode == bulk`, `train until reps == 5`). I also asked about `===` as an alternative; AI correctly noted FitLang has no reference types, so JS-style identity vs value equality would be ceremony. Ended at `==` and `!=`, with semantics deferred to §4.7.
- **String-literal escapes extended.** AI's first draft allowed only `\"` and `\\`. I added `\n` — meal/exercise notes occasionally need a line break in a label.
- **AI quietly added `+ - *` to the punctuation alphabet** during the macro/equality fix and flagged it for review. Accepted — §4.1 already references `80 kg + 1 m` as a type-error example, so the arithmetic operators need lexical existence even if §4.7 specifies semantics.
- **Compound rate `kg/week` as a single lexeme was rejected** in favor of three tokens (`UNIT` `/` `UNIT`) — consistent with option B and avoids a non-regular sub-rule in the lexer.

## Errors I caught
1. **Over-strict family split (Macro vs Mass).** The AI's first draft made `g` a Macro-only unit, which would have made `bodyweight: 70000 g` a type error — strict for no reason since grams *are* mass. If I hadn't pushed back, §4.5 would have inherited an artificial four-family split with no domain justification, and the "every design choice must trade something off" §4.8 paragraph would have been weaker.
2. **Missing equality operator.** AI argued from `target`/`goal` only needing `>=`/`<=` and concluded no `==` was needed — but predicates and conditionals (which we will need for §4.4 semantics and §4.7 expressions) require equality. Catching it now is cheaper than discovering it mid-§4.7 and having to amend §4.2 retroactively.
3. **AI initially drafted §4.2 without asking which lexicon shape we wanted** — it had a recommendation in mind and would have written closed-lexicon-with-`let` directly. I asked it to surface the trade-off explicitly first, which forced a comparison table I could actually evaluate. This is a workflow lesson: the AI defaults to drafting from its own picks unless prompted to expose decisions.

## Reflection
Today I couldn't do much, we went throgh the lexical structure of the language. We discussed about the lexicon shape, comments, reserved word list etc. the AI gave good suggestions and I gave mine too and we agreed and we are ready for the EBNF section.
