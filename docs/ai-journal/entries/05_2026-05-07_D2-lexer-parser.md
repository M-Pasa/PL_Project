---
entry: 05
date: 07-05-2026
phase: Parser
ai_tool: Claude Sonnet 4.6 (claude-sonnet-4-6) via Claude Code CLI
---

# Entry 05 — D2: Lexer, Parser, and AST Dump (Java)

## Goal

Implement D2 for the Part 1 submission: a FitLang lexer and recursive-descent parser in Java, with a `--dump-ast` flag that pretty-prints the abstract syntax tree. Also write the 3 valid and 5 malformed sample programs for D3.

## Prompt

Multi-turn session. Opening:

> *"let's continue on the project"*

After the AI summarised the remaining Part 1 deliverables (D2 missing entirely, D3 missing, D4 E1 not done, D5 not done), I chose D2 first. The AI proposed the Java source structure before writing any code and I approved it.

## Response (key part)

The AI produced seven Java source files, a `compile.bat`, a `README.md`, and eight sample `.fl` programs in one session. Key files:

- `src/fitlang/Token.java` — `Token.Type` enum covering all reserved words, mode literals, unit tokens, punctuation, and `EOF`. `week` is a single token type (serves both reserved-word and unit roles as specified in D1 §4.2.6).
- `src/fitlang/Lexer.java` — scanner with maximal-munch two-character punctuation (`>=`, `<=`, `==`, `!=`), `#` line comment stripping, closed-lexicon keyword/unit table lookup before emitting `IDENT`.
- `src/fitlang/ast/Ast.java` — all AST node types as inner static classes: `AthleteBlock`, `WorkoutDecl`, `ExerciseDecl`, `ProgressStmt`, `MealDecl`, `MacroDecl`, `TargetDecl`, `GoalRateDecl`, `LetDecl`, `ScheduleStmt`, `Quantity`, `Rate`, and the `Expr` hierarchy (`QuantityExpr`, `NumExpr`, `IdentExpr`, `BinaryExpr`).
- `src/fitlang/Parser.java` — hand-written recursive descent; one method per §4.3 nonterminal; one-token lookahead throughout; `parseQuantity()` and `parseRate()` fuse multi-token parser-level constructs; `parseTerm()` looks one position ahead to distinguish `NUM_LIT` from `<quantity>`.
- `src/fitlang/AstDumper.java` — indented text tree printer for `--dump-ast`.
- `src/fitlang/Main.java` — entry point; reads file, runs lexer + parser, prints AST if `--dump-ast`, exits 1 on error.

Compiled clean on Java 21 (`javac 21.0.9`). All 3 valid programs parsed correctly; all 5 malformed programs produced the expected error messages with line and column numbers.

Sample `--dump-ast` output for `valid_01_full.fl`:

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

## Accepted

- All seven source files as produced. The grammar-to-method mapping in `Parser.java` is direct and matches D1 §4.3 nonterminal names closely enough to serve as a cross-reference.
- Single `WEEK` token type for the dual reserved-word/unit role — consistent with D1 §4.2.6 and avoids an artificial scanner split.
- All AST nodes as inner static classes of one `Ast.java` file — reduces the file count without losing type safety or legibility.
- `parsePositiveInt()` helper that throws with the token's own line/col — better error location than a generic catch.

## Rejected / modified

- **AI attempted to pre-write the E1 experiment journal entry (entry 05) before this one.** It generated an EBNF from a paragraph description, then fabricated the "Accepted / Rejected / Errors I caught / Reflection" sections as if I had evaluated the output and formed opinions — which I had not done. I rejected the entire entry and deleted the file. The E1 experiment will be run as a real interactive session in the next session.

## Errors I caught

1. **Fabricated journal entry.** The AI drafted journal entry 05 as the E1 experiment without running the experiment interactively, filling in evaluation sections (Accepted, Rejected, Errors, Reflection) that represent things I never said or decided. A graded AI journal entry that the student cannot defend is worse than no entry. Caught on review; file deleted before committing.

## Reflection

Today we worked on the code part of the design phase, we did the lexer, parser, tokenizer etc. and we verified them with sample codes with valid and invalid syntax and everything went fine. The AI was doing well, I still have to review the code but for now it looks fine.