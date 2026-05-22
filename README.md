# FitLang — D2: Lexer, Parser, Type Checker, Interpreter

**Author:** Muhammed Paşa (220104004930)
**Course:** CSE 341 — Concepts of Programming Languages, Spring 2026, Gebze Technical University
**Deliverable:** D2 (Part 2)

> **AI use:** the handout requires AI assistance and a documented journal (D4). My journal — including which tools were used, raw experiment transcripts, and how the entries were written — is at [`docs/ai-journal/`](docs/ai-journal/README.md).

---

## Overview

FitLang is a domain-specific language for writing personal strength-training programs together with the nutrition required to execute them. The grammar is specified in D1 §4.3; the static type system (unit-aware Mass / Energy / Time with cross-family rejection) in §4.5; the big-step operational semantics in §4.4; the expression sublanguage in §4.7.

The implementation is a hand-written **recursive-descent LL(1) parser** in Java 11+, a **type checker** that enforces dimensional families and the no-shadowing / no-forward-reference rules of §4.6, and a **tree-walking interpreter** that materialises an N-week `Plan` value with per-week bodyweight projection, exercise progression, and per-target macro checks.

---

## Requirements

- Java 11 or later (`javac`, `java`)

Check with:

```
java -version
```

---

## Building

From the repository root:

```bat
compile.bat
```

Or manually:

```bat
mkdir out
javac -d out src\fitlang\FitLangException.java src\fitlang\Token.java src\fitlang\ast\Ast.java src\fitlang\Lexer.java src\fitlang\Parser.java src\fitlang\AstDumper.java src\fitlang\TypeChecker.java src\fitlang\Interpreter.java src\fitlang\Main.java
```

Compiled classes go into `out\`.

---

## Running

```
java -cp out fitlang.Main <file.fl>
```

Parses, type-checks, and reports `OK` or the first parse / type error with line and column.

```
java -cp out fitlang.Main <file.fl> --dump-ast
```

Adds the AST as an indented text tree (after type-checking).

```
java -cp out fitlang.Main <file.fl> --run
```

After type-checking, interprets the program and prints the materialised `Plan` (per-week bodyweight, evaluated workouts with progressed weights, target reports with planned vs threshold).

```
java -cp out fitlang.Main <file.fl> --no-check
```

Parses only, skipping the type checker. Useful when inspecting an intentionally ill-typed program.

Exit code is `0` on success, `1` on any error (parse, type, or runtime).

---

## Example

Given `samples/valid_01_full.fl`:

```
java -cp out fitlang.Main samples\valid_01_full.fl --run
```

prints the full `Plan` with one week, the Push Day workout, the Breakfast meal, and the protein-per-kg target report. Replace `--run` with `--dump-ast` to inspect the parsed tree instead.

---

## Error reporting

Three diagnostic phases, each with a distinct prefix:

```
Parse error: Workout "Empty Day" must contain at least one exercise [line 5, col 5]
Type errors in 'foo.fl':
  - cross-family arithmetic: Mass + Energy at line 12, col 9
Runtime error: bodyweight became negative at week 35 (78.0 kg - 2.5 kg/week)
```

Runtime checks cover division by zero, non-negative exercise weights after progression (R1), and positive projected bodyweight across the planned horizon (R2). See D1 §4.4.3.

---

## Source layout

```
src/
  fitlang/
    Main.java              entry point; --dump-ast / --run / --no-check flags
    Token.java             token class; Token.Type enum (all lexical categories)
    Lexer.java             scanner — UTF-8 source -> token list
    Parser.java            recursive-descent LL(1) parser -> AST
    AstDumper.java         indented-text pretty-printer for --dump-ast
    TypeChecker.java       D1 §4.5 + §4.6 static checks (units, scope, shadowing)
    Interpreter.java       big-step interpreter; produces Plan (§4.4.3)
    FitLangException.java  parse / type / runtime error with line/col
    ast/
      Ast.java             all AST node types as inner static classes

samples/
  valid_01_full.fl              full athlete block (workout + nutrition + rules)
  valid_02_workout_only.fl      workout-only program (no meal section)
  valid_03_diet_only.fl         diet-only program (no workout section)
  valid_04_multi_week.fl        plan N week with bodyweight + exercise progression
  valid_05_expressions.fl       §4.7 expression-bodied slots (let, arithmetic)
  valid_06_comprehensive.fl     routines, when-dispatch, per-kg targets together
  valid_07_cut_branch.fl        cut goal + lose direction + when-else dispatch
  malformed_01_empty_body.fl        no workout/meal in athlete block
  malformed_02_empty_workout.fl     workout block with no exercises
  malformed_03_missing_schedule.fl  no plan statement
  malformed_04_bad_goal_mode.fl     invalid goal mode keyword
  malformed_05_unterminated_string.fl  string literal not closed
  invalid_typeerror.fl          Mass + Energy (cross-family static type error)
  invalid_shadow.fl             let redeclares a bound name (§4.6 violation)
  runtime_negative_weight.fl    progression drives exercise weight below 0 (R1)
  runtime_negative_bodyweight.fl  multi-week loss exceeds initial bodyweight (R2)
```

---

## Design notes

- **Closed lexicon** (D1 §4.2.3): every alphabetic lexeme is either a reserved word, a mode literal, or a unit — the lexer checks them in that order before emitting IDENT. Keywords can never be re-used as identifiers.
- **`week` is dual** (D1 §4.2.4 / §4.2.5): the scanner emits a single `WEEK` token; the parser accepts it as both a reserved word (in `plan week with`) and a time-family unit (in rates like `2.5 kg/week`).
- **Quantities are parser-level pairs** (D1 §4.3.2): the lexer emits `NUM_LIT` and `UNIT` separately; `parseQuantity()` fuses them and attaches the dimensional family.
- **One-token lookahead** everywhere: each production alternative is selected by a unique token in its FIRST set; no backtracking is needed.
- **Control structure** (D1 §4.3.2 `<when-decl>`): `when goal: <mode> { ... } [ else { ... } ]` selects between two rule sub-blocks based on the static goal mode. Dispatch is decidable at parse time and the unused branch is dropped before type-checking.
- **User-defined templates** (D1 §4.3.2 `<routine-decl>` / `<routine-call>`): `routine "Name"(p1: kg, p2: kg) { ... }` declares a parameterised exercise group at athlete scope; `use "Name"(82 kg, 22 kg)` instantiates it inside a `<workout-decl>` body. Routine declarations precede `<body-sections>` so calls always resolve to an earlier declaration.
- **Unit-aware type system** (D1 §4.5): within-family unit coercion (kg ↔ g, kcal ↔ kJ, week ↔ s) canonicalises every quantity to its base unit; cross-family arithmetic (Mass + Energy, kg × kg) is a static type error.
- **Multi-week unfolding** (D1 §4.4.3): `plan N week` materialises N `Week` values; per-week bodyweight is `bw₀ + goalRate × (k − 1) week`, per-exercise weight is `w₀ + progress × (k − 1) week`, per-target threshold uses the week-k bodyweight when the target is `per kg of bodyweight`.
- **Domain-specific runtime checks** (D1 §4.4.3): the interpreter aborts on division by zero, on a negative exercise weight after progression (R1), and on a non-positive projected bodyweight at any week in the horizon (R2). Each fault names the offending week and value.
