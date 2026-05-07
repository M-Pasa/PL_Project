# FitLang — D2: Lexer + Parser

**Author:** Muhammed Paşa (220104004930)  
**Course:** CSE 341 — Concepts of Programming Languages, Spring 2026, Gebze Technical University  
**Deliverable:** D2 (Part 1)

---

## Overview

This directory contains the FitLang lexer and parser (D2). FitLang is a domain-specific language for writing personal strength-training programs together with the nutrition required to execute them. The grammar is specified in D1 §4.3.

The implementation is a hand-written recursive-descent parser (LL(1)) in Java 11+, matching the grammar class claimed in D1 §4.3.5.

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
javac -d out src\fitlang\FitLangException.java src\fitlang\Token.java src\fitlang\ast\Ast.java src\fitlang\Lexer.java src\fitlang\Parser.java src\fitlang\AstDumper.java src\fitlang\Main.java
```

Compiled classes go into `out\`.

---

## Running

```
java -cp out fitlang.Main <file.fl>
```

Parses the given `.fl` file and reports success or the first parse error with line and column number.

```
java -cp out fitlang.Main <file.fl> --dump-ast
```

Parses the file and prints the abstract syntax tree as an indented text tree.

---

## Example

Given `samples/valid_01_full.fl`:

```
java -cp out fitlang.Main samples\valid_01_full.fl --dump-ast
```

Output:

```
AthleteBlock
  AthleteParams
    bodyweight : 78.0 kg  [MASS]
    goal       : bulk
  WorkoutDecl "Push Day"
    ExerciseDecl "Bench Press"
      sets   : 4
      reps   : 6
      weight : 80.0 kg  [MASS]
    ExerciseDecl "Overhead Press"
      sets   : 3
      reps   : 8
      weight : 50.0 kg  [MASS]
    ProgressStmt rate=2.5 kg/week
  MealDecl "Breakfast"
    MacroDecl protein : 40.0 g  [MASS]
    MacroDecl carbs : 80.0 g  [MASS]
    MacroDecl fat : 15.0 g  [MASS]
  RulesSection
    TargetDecl protein >= 1.8 g per kg of bodyweight
    GoalRateDecl lose 0.5 kg/week
  ScheduleStmt
    workouts : ["Push Day"]
    meals    : ["Breakfast"]
```

---

## Error reporting

Parse errors include the error description, line number, and column number:

```
Parse error: Workout "Empty Day" must contain at least one exercise [line 5, col 5]
```

Exit code is `0` on success, `1` on parse error.

---

## Source layout

```
src/
  fitlang/
    Main.java           entry point; handles --dump-ast flag
    Token.java          token class; Token.Type enum (all lexical categories)
    Lexer.java          scanner — UTF-8 source → token list
    Parser.java         recursive-descent LL(1) parser → AST
    AstDumper.java      indented-text pretty-printer for --dump-ast
    FitLangException.java  parse error with line/col
    ast/
      Ast.java          all AST node types as inner static classes

samples/
  valid_01_full.fl         full athlete block (workout + nutrition + rules)
  valid_02_workout_only.fl workout-only program (no meal section)
  valid_03_diet_only.fl    diet-only program (no workout section)
  malformed_01_empty_body.fl        no workout/meal in athlete block
  malformed_02_empty_workout.fl     workout block with no exercises
  malformed_03_missing_schedule.fl  no plan statement
  malformed_04_bad_goal_mode.fl     invalid goal mode keyword
  malformed_05_unterminated_string.fl string literal not closed
```

---

## Design notes

- **Closed lexicon** (D1 §4.2.5): every alphabetic lexeme is either a reserved word, a mode literal, or a unit — the lexer checks them in that order before emitting IDENT. Keywords can never be re-used as identifiers.
- **`week` is dual** (D1 §4.2.6): the scanner emits a single `WEEK` token; the parser accepts it as both a reserved word (in `plan week with`) and a time-family unit (in rates like `2.5 kg/week`).
- **Quantities are parser-level pairs** (D1 §4.3.4): the lexer emits `NUM_LIT` and `UNIT` separately; `parseQuantity()` fuses them and attaches the dimensional family.
- **One-token lookahead** everywhere: each production alternative is selected by a unique token in its FIRST set; no backtracking is needed.
