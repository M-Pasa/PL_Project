---
entry: 04
date: 05-05-2026
phase: Design
ai_tool: Claude Opus 4.7 (claude-opus-4-7) via Claude Code CLI
---

# Entry 04 — D1 §4.3 Syntax (EBNF) and §4.6 Names/Binding/Scope/Lifetime

## Goal
Lock down the two remaining P1 sections of D1: the context-free grammar (§4.3) and the names/binding/scope/lifetime story (§4.6). Decide the shape of a FitLang program at the start symbol, settle whether assignment exists, and tie the scoping rule to the language's domain commitments.

## Prompt
Multi-turn working session. Opening prompt:

> *"good, I guess we were about to write the EBNF rules, am I right?"*

Subsequent turns negotiated four design calls before drafting: EBNF flavor (Sebesta-style `→` vs ISO 14977), top-level program shape (structured Pascal-style vs flat C/Python list), what `<schedule-stmt>` looks like for a diet-only client, and whether P1's `<expression>` should have operators.

## Response (key part)
The AI proposed two alternatives for the start symbol with named real-language precedents:
- **Option A (Pascal-style):** `<program> → "program" identifier <goal-section> <plan-section> <log-section> "end"` — fixed skeleton, sections in required order. Pascal, Ada.
- **Option B (C/Python-style):** `<program> → declaration { declaration }` — flat list. C translation units, Python files.

It recommended A on the originality argument: "a general-purpose language can't bake its program shape into the start symbol because it does not know what a program is *for*; FitLang does, so it should." The grammar itself then expresses the domain commitment that a FitLang program is a coherent goal+plan+log triple, rather than deferring it to a semantic check.

The drafted §4.3 used `<athlete-block>` as the start symbol rather than the literal Pascal `program ... end` skeleton (because §4.1's existing sample program already uses `athlete(...) { ... }`), with `<plan-section>`, `<intake-section>`, `<rules-section>`, `<schedule-stmt>` as required children in fixed order.

§4.6 was structured around the immutability spine: two disjoint name spaces (string labels for workouts/meals/exercises, `IDENT` for `let` bindings), all bindings statically bound (Sebesta §5.4.1), static lexical scope with three nesting levels, lifetime collapses to one of Sebesta's four storage categories because no mutation can distinguish the others, no shadowing.

## Accepted
- **Option A — structured Pascal-style start symbol.** The originality argument is sharp and the AI's framing — "the grammar itself expresses the domain commitment" — is exactly the kind of paragraph §4.8's rationale section needs. Also pays off downstream: §4.5's type system can assume a single goal context exists without a scope-lookup step.
- **Sebesta-style EBNF notation** (`→`, `{}`, `[]`, `|`) over ISO 14977. Chosen explicitly to score on the "Use of Sebesta Framework" rubric dimension — matching the textbook's notation throughout the document is cheap and visible.
- **LL(1) claim with FIRST-set justification.** Every alternative inside `<rule-decl>` and `<athlete-block>`'s body is selected by a unique reserved word in the FIRST set (`workout`, `meal`, `target`, `goal`, `let`, `progress`, `plan`). No left recursion. This commits D2 to a hand-written recursive-descent parser with a clear implementation path.
- **`<quantity>` and `<rate>` as parser-level fusions** of multiple lexer tokens, not as single lexemes. Consistent with §4.2's option B from the previous session — the lexer never has to know unit families.
- **Two disjoint name spaces** (`STRING_LIT` labels vs `IDENT` let-bindings). The AI cited Common Lisp's separate function/value namespaces (Sebesta §5.2) as precedent — the parser never has to disambiguate, and "Push Day" never collides with any user identifier.
- **Static binding everywhere, no dynamic scoping.** AI rejected Sebesta §5.5.2 dynamic scoping outright: "the DSL has no first-class procedures and no use case that would justify the cost." Correct.
- **Forward references forbidden.** A `<let-decl>` may only reference earlier `let`s; rules out mutual recursion among bindings, which the domain has no use for, and lets the type checker run in a single linear pass.
- **`bodyweight` as an ALGOL-style special-word handle**, not an `IDENT`. Sidesteps the question of what to do if a user wrote `let bodyweight = 90 kg`.
- **Immutability + named-constants framing** (Sebesta §5.8). The AI's "a fitness program is a *plan*, not a process" line is the right rationale paragraph for §4.8.

## Rejected / modified
- **Workout+meal both required was rejected.** AI's first draft of §4.3 made both `<plan-section>` and `<intake-section>` mandatory. I pushed back with the elderly-client-on-a-diet case: a fitness+nutrition DSL shouldn't force a workout. AI agreed and refactored to `<body-sections> → <plan-section> [ <intake-section> ] | <intake-section>` — at least one required, both allowed in fixed order, neither alone-and-empty. Workout-only and meal-only programs are now first-class.
- **`<schedule-stmt>` was over-constrained the same way.** Required `workouts: [...]` *and* `meals: [...]`. Same fix: factored into `<schedule-clauses>` with two alternatives so workouts-only and meals-only schedules parse.
- **Empty `<expression>` stub was rejected.** AI initially proposed `<expression> → <quantity> | NUM_LIT | IDENT` — pure aliasing only. I pointed out this makes `let` useless: you can't write `let total = breakfast + lunch`. AI added additive operators (`+`, `-`) with explicit reasoning that they're associative/commutative so precedence isn't an issue, and §4.7 in P2 cleanly extends with multiplicative ops + grouping. P1 `let` is now actually useful.
- **No assignment operator (confirmed, not added).** I asked whether the language has assignment. AI's answer was correct and well-justified: `=` in `<let-decl>` is binding, `:` in record literals is field initialisation, `==`/`>=` are relational. Mutation has no domain meaning ("you don't mutate Push Day mid-execution — if you change it, you've defined a different workout"). Single-assignment discipline cited to Erlang and Rust's `let` without `mut`. Worth recording explicitly because it's exactly the kind of design decision Exam 1 might ask me to defend.
- **Shadowing was forbidden, stricter than C/Java.** AI proposed and I accepted. With immutable bindings and a single flat scope for `let`, shadowing buys nothing and only invites the "which `dailyKcal` did I mean?" class of bug. Justified to be stricter because FitLang's nesting is a fixed three levels deep — we can afford a stricter rule that a deeper-nesting language could not.

## Errors I caught
1. **Over-rigid program shape.** AI's first §4.3 draft locked in `<plan-section>` *and* `<intake-section>` as both mandatory, despite us having just discussed that FitLang serves diet-only and workout-only users. If I hadn't raised the elderly-client case, the grammar would have rejected an entirely legitimate class of programs and §4.8's rationale paragraph about "the grammar itself enforces domain commitments" would have been undermined by the obvious counter-case at the exam.
2. **Empty operator layer.** AI deferred *all* expression structure to §4.7 (P2), leaving `let` syntactically pure aliasing in P1. I caught this on review — it would have meant D3's three valid programs couldn't show off any computation, and the §4.5 type-rule examples in P2 would have had to retrofit operators into the grammar. Adding `+`/`-` to P1 with the explicit "multiplicative operators deferred" note is the cleaner scope split.
3. **AI volunteered to draft §4.3 immediately on the EBNF flavor question** instead of surfacing the start-symbol decision first. I asked "what is the second one" (top-level program shape) and that forced the structured-vs-flat tradeoff into the open. Same workflow lesson as last session: the AI defaults to drafting from its own picks unless prompted to expose the design decisions first.

## Reflection
Today we discussed the program shape. The AI agent was thinking of making the workout essential for the program to parse when I was thinkin on all sides diet only, workout only, both diet and workout.Currently we decided to not have assingment operato, I am still not sure about that the AI agent says that the scope of the language does not require it but I think it might be useful. For now I am convinced that it is not needed for our case but later on we may introduce it we will see over time.