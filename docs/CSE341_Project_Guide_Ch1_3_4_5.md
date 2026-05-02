# CSE 341 — DSL Project Guide (Sebesta Ch. 1, 3, 4, 5)

> **Purpose of this document.** This is a reference guide for an AI agent assisting on the CSE 341 DSL design project (Part 1). It distills the material from Sebesta's *Concepts of Programming Languages* (12th ed.), Chapters **1, 3, 4, 5**, which are the chapters the Part 1 written exam covers and the chapters that the Part 1 D1 sections (4.1 Language Overview, 4.2 Lexical Structure, 4.3 Syntax, 4.6 Names/Binding/Scope/Lifetime) must cite.
>
> **Source of truth: the book.** Every concept, term, and rule below is from Sebesta. Do not invent design rules that are not in the book. When in doubt, search the book first.
>
> **How to use this file.** Each top-level section maps to one Sebesta chapter. Inside each chapter, subsections follow the book's section numbering so the agent can cite §1.3, §3.3, §4.2, §5.4, etc. directly in the D1 document. The final section ("Project Mapping") shows which book section each Part 1 deliverable section must cite.

---

## Table of Contents

1. [Chapter 1 — Preliminaries (Language Evaluation Criteria)](#chapter-1)
2. [Chapter 3 — Describing Syntax and Semantics](#chapter-3)
3. [Chapter 4 — Lexical and Syntax Analysis](#chapter-4)
4. [Chapter 5 — Names, Bindings, and Scopes](#chapter-5)
5. [Project Mapping (Sebesta → D1 sections)](#project-mapping)

---

<a id="chapter-1"></a>
# Chapter 1 — Preliminaries

This chapter is the source of vocabulary for **D1 §4.1 (Language Overview)**, where you must cite Sebesta's evaluation criteria and say which your DSL prioritizes and which it knowingly sacrifices.

## 1.2 Programming Domains
Sebesta groups languages by application domain. When you justify the *domain* of your DSL, this is the relevant taxonomy:

- **Scientific applications** — early dominant domain; simple data structures, lots of floating-point, arrays/matrices, counting loops; Fortran is the dominant language.
- **Business applications** — elaborate reports, decimal arithmetic, character data; COBOL is dominant.
- **Artificial intelligence** — symbolic rather than numeric computation; linked lists; dynamic creation/execution of code segments; Lisp historically, Prolog for logic-style AI, Python more recently.
- **Web software** — markup + general-purpose programming; commonly via embedded scripting (JavaScript, PHP).
- **Systems programming**, embedded systems, scripting, and special-purpose are also recognized.

A DSL does not need to fit neatly into one of these — but the **rationale** for picking your domain (writability inside this niche, readability for domain experts, etc.) should be expressed in Sebesta's vocabulary.

## 1.3 Language Evaluation Criteria — the four primaries
Sebesta's four primary criteria. **Memorize these names exactly** — the exam expects them.

1. **Readability** (§1.3.1) — ease with which programs can be read and understood.
2. **Writability** (§1.3.2) — measure of how easily a language can be used to create programs *for a chosen problem domain*. Writability must always be discussed relative to a domain.
3. **Reliability** (§1.3.3) — a program is reliable if it performs to its specifications under all conditions.
4. **Cost** (§1.3.4) — total cost of the language, broken into multiple sub-costs.

> Of these, Sebesta states explicitly that **writability and readability are the two most important**, because the largest cost contributors (development, maintenance, reliability) all depend on them.

### 1.3.1 Readability — characteristics that affect it
- **Overall simplicity** — too many constructs, too much feature multiplicity, or operator overloading can hurt readability. Simplicity carried too far (e.g. assembly) also hurts readability because programs are too long and lack visible structure.
- **Orthogonality** — a small set of primitive constructs combined in a small number of consistent ways; every combination is legal and meaningful. *Too much* orthogonality (ALGOL 68) leads to combinatorial explosion and complexity. Too little (C — e.g. records can be returned from functions but arrays cannot) leads to exception rules.
- **Data types** — adequate built-in types and ability to define new ones aids readability (Sebesta's example: `timeout = 1` vs `timeout = true`).
- **Syntax design** — special words, form-and-meaning correspondence, the way compound statements are bracketed (Ada's `end if` / `end loop` is more readable than C's matched braces), whether special words can be used as variable names.

### 1.3.2 Writability — characteristics that affect it
- **Simplicity and orthogonality** (same factors as readability — but too much orthogonality hurts writability too because errors go undetected when nearly everything is legal).
- **Expressivity** — convenient ways to express computations (`count++` vs `count = count + 1`; short-circuit `and then`; `for` for counting loops).
- **Support for abstraction** — process abstraction (subprograms) and data abstraction.

### 1.3.3 Reliability — characteristics that affect it
- **Type checking** — compile-time type checking is more reliable than run-time; run-time is more reliable than none. Costly bugs are detected earlier.
- **Exception handling** — language-level mechanism to intercept and respond to run-time errors raises reliability.
- **Aliasing** — two or more variable names bound to the same memory cell. Hurts reliability. Some languages restrict it; some require it (pointers, parameter passing).
- **Readability and writability both affect reliability** — a language that supports natural expression of an algorithm is more likely to be used to write a correct program.

### 1.3.4 Cost — five sub-costs
1. Cost of **training programmers** (function of simplicity/orthogonality + their experience).
2. Cost of **writing programs** (function of writability + closeness to application domain).
3. Cost of **executing** programs (function of design — e.g. mandatory run-time checks, optimization vs compile time trade-off).
4. Cost of **poor reliability** (failure in critical systems → catastrophic; lawsuits; lost business).
5. Cost of **maintaining** programs (corrections + new functionality; primarily a function of *readability*; for long-lived large systems, maintenance is 2–4× development cost).

> Other criteria mentioned: **portability** (function of standardization), **generality** (range of applications), **well-definedness** (precision of the defining document). These are secondary.

## 1.6 Language Design Trade-offs
The criteria are mutually contradictory — Hoare (1973) called reconciling them "a major engineering task."
Two canonical examples Sebesta gives:

- **Reliability vs. cost of execution** — Java mandates array index range checking → reliable but slower. C does not → faster but less reliable.
- **Writability vs. readability** — APL has many powerful operators → very writable for array work but very poorly readable.

> When you write your D1 §4.8 (Design Rationale) you are *expected* to identify a trade-off you accepted for each of your design decisions. This section of Sebesta is the reason for that requirement.

## 1.5 Language Categories (brief)
Imperative, functional, logic, object-oriented. Markup-programming hybrids (XSLT, JSP) and scripting languages also discussed but not their own pure categories.

## 1.7 Implementation Methods (brief — relevant because your D2 is an interpreter)
- **Compilation** — translate to machine code, fast execution.
- **Pure interpretation** — no translation, software simulates the machine; 10–100× slower; great error messages; rare for high-level languages today (but: JavaScript, PHP).
- **Hybrid** — translate to intermediate code, then interpret it (Perl; classic Java); JIT compilers turn hybrid into delayed compilation.

> Your DSL interpreter (D2) is essentially a pure interpreter (or a hybrid if you produce an AST then walk it — which Sebesta would call the typical hybrid form).

---

<a id="chapter-3"></a>
# Chapter 3 — Describing Syntax and Semantics

This is the chapter behind **D1 §4.3 (Syntax — EBNF grammar)** and the foundation for D1 §4.4 (Semantics, Part 2 only). For Part 1, the syntax / formal grammar parts are essential.

## 3.1–3.2 Syntax vs. Semantics; Lexemes and Tokens
- **Syntax** — the *form* of the language's expressions, statements, and program units.
- **Semantics** — the *meaning* of those constructs.
- A **language** is a set of strings of characters from some alphabet. Strings of the language are called **sentences** or **statements**.
- The lowest-level syntactic units are **lexemes** (the actual character substrings: `index`, `=`, `2`, `*`, `count`, `+`, `17`, `;`).
- Lexemes are partitioned into groups; each group has a name called a **token** (e.g. `identifier`, `int_literal`, `equal_sign`, `mult_op`, `plus_op`, `semicolon`). A token is a *category* of lexemes.
- Some tokens have only one possible lexeme (e.g. the token for `+`).

### 3.2.1–3.2.2 Two ways to formally define a language
- **Recognition** — a device R (e.g. a parser) reads a string and decides if it is in L.
  > The syntax analyzer of a compiler is a recognizer.
- **Generation** — a device that produces sentences of L.
  > Grammars (BNF) are generators. Generators are easier for humans to read.

## 3.3 Formal Methods of Describing Syntax

### 3.3.1 BNF and Context-Free Grammars
- **Chomsky** (mid-1950s) defined four classes of grammars; two are useful here:
  - **Regular grammars** — describe tokens (lexical structure).
  - **Context-free grammars** — describe (almost all of) program syntax.
- **BNF** (Backus–Naur Form), introduced for ALGOL 60, is essentially equivalent to context-free grammars.
- A **metalanguage** is a language used to describe another language. BNF is a metalanguage.

### 3.3.1.3 BNF fundamentals
- **Nonterminal** (a.k.a. abstraction) — the named syntactic categories on the LHS, written `<like_this>`.
- **Terminal** — lexemes / tokens on the RHS.
- A **rule** (a.k.a. production) has form `LHS → RHS`. RHS may mix terminals and nonterminals.
- Multiple alternatives can be combined with `|`:
  ```
  <if_stmt> → if ( <logic_expr> ) <stmt>
            | if ( <logic_expr> ) <stmt> else <stmt>
  ```
- A **grammar** is a collection of rules.

### 3.3.1.4 Describing lists in BNF
Use **recursion** (a rule whose LHS appears in its RHS):
```
<ident_list> → identifier
             | identifier , <ident_list>
```

### 3.3.1.5 Derivations
- A **derivation** is a sequence of rule applications, starting from the **start symbol** (the special root nonterminal), each step rewriting one nonterminal using one of its rules.
- A **leftmost derivation** rewrites the leftmost nonterminal at each step. **Rightmost** is symmetric. Either yields the same parse tree for an unambiguous grammar.
- A **sentential form** is any string produced during derivation (terminals + nonterminals); a **sentence** is a sentential form with only terminals.

### 3.3.1.6–3.3.1.7 Parse Trees and Ambiguity
- A **parse tree** is the hierarchical structure that a derivation describes; internal nodes are nonterminals, leaves are terminals.
- A grammar is **ambiguous** if some sentence has two or more distinct parse trees.
- **Why ambiguity is a problem**: compilers often base semantics on parse-tree shape; ambiguity → no unique meaning.
- It is mathematically *undecidable* in general whether an arbitrary grammar is ambiguous.

### 3.3.1.8 Operator Precedence and Associativity in BNF
- Encode precedence by **layering** nonterminals: lower-precedence operators at the top (e.g. `<expr>`), higher-precedence below (`<term>`, `<factor>`). Example (the canonical Sebesta precedence grammar):
  ```
  <expr>   → <expr> + <term> | <term>
  <term>   → <term> * <factor> | <factor>
  <factor> → ( <expr> ) | <id>
  ```
- **Left-recursive** BNF rules (like `<expr> → <expr> + <term>`) **force left associativity**.
- The **dangling else** problem: an ambiguous if/else is resolved by splitting statements into `<matched>` and `<unmatched>`; an `else` always binds to the nearest unmatched `then`.

### 3.3.2 Extended BNF (EBNF)
EBNF adds three notational conveniences (no extra descriptive power, just readability):

| Notation | Meaning |
|----------|---------|
| `[ X ]` | `X` is optional (zero or one). |
| `{ X }` | `X` may repeat zero or more times. |
| `( A \| B \| C )` | grouped alternatives. |

EBNF example for an if statement and an identifier list:
```
<if_stmt>   → if ( <expression> ) <statement> [ else <statement> ]
<ident_list> → ident { , ident }
```

> **Important caveat** (Sebesta §3.3.2): the EBNF rule `<expr> → <term> { + <term> }` does **not** specify associativity direction. The recursion in `<expr> → <expr> + <term>` does. With EBNF, the *parser* enforces associativity.

Some EBNF variants use `:` instead of `→`, place RHSs on separate lines, use `{X}+` for one-or-more, and the ISO/IEC 14977 standard (rarely used in practice) uses `=`, `;`, and quoted terminals.

### 3.3.3 Grammars and Recognizers
For every context-free grammar there is a recognizer (parser) that can be algorithmically constructed. Tools like **yacc** automate this; this is the bridge to Chapter 4.

## 3.4 Attribute Grammars (background only — not required for D1 Part 1)
Attribute grammars extend context-free grammars with **attributes**, **attribute computation functions** ("semantic functions"), and **predicate functions** describing **static semantics**. Useful for things BNF cannot express on its own — e.g. "all variables must be declared before use" or type compatibility.

- **Static semantics** — language rules checkable at compile time but not expressible in BNF (typing rules, declaration-before-use).
- **Synthesized attributes** — pass info up the parse tree.
- **Inherited attributes** — pass info down/across the tree.
- **Predicates** — Boolean expressions over attributes; a false predicate signals a static-semantics violation.
- **Intrinsic attributes** — synthesized attributes of leaf nodes whose values come from outside the parse tree (e.g. the symbol table).

## 3.5 Dynamic Semantics (relevant in Part 2 only)
The **dynamic semantics** is the meaning of expressions, statements, and program units at run time. Three formal approaches:

- **Operational semantics** (§3.5.1) — define meaning by describing the effect of running the construct on an idealized machine, viewed as a sequence of state changes. Works well for users and implementors when kept informal. Used in the project example ("ShapeScript") in the handout.
- **Denotational semantics** (§3.5.2) — define meaning by mapping each language construct to a mathematical object (often a function) via recursive functions. Most rigorous and most widely known; based on recursive function theory. The state is a set of `<variable, value>` pairs; `VARMAP(i, s) = v` looks up the value of `i` in state `s`. State-changing constructs map states to states; expressions map states to values.
- **Axiomatic semantics** (§3.5.3) — based on mathematical logic; uses preconditions and postconditions. **NOT accepted for this project** (per the handout §4.4).

> For Part 2 you must give either operational or denotational semantics for at least two non-trivial constructs.

---

<a id="chapter-4"></a>
# Chapter 4 — Lexical and Syntax Analysis

This chapter is the basis for **D1 §4.2 (Lexical Structure)** and for the implementation work in **D2 (Lexer + Parser)**.

## 4.1 Why separate lexical from syntax analysis
Three reasons (§4.1):
1. **Simplicity** — lexical analysis techniques are simpler; isolating them keeps the parser smaller and cleaner.
2. **Efficiency** — lexical analysis is a significant fraction of compile time and benefits from special optimization; the parser does not.
3. **Portability** — the lexer reads files and is platform-dependent; isolating that lets the parser stay portable.

## 4.2 Lexical Analysis
- A **lexical analyzer** (lexer) is a **pattern matcher**. It reads the input character stream and produces a stream of (token, lexeme) pairs.
- Modern lexers are subprograms: each call returns the next lexeme + token, on demand from the parser.
- The lexer **skips whitespace and comments** outside lexemes, **inserts user-defined names into the symbol table**, and **detects ill-formed tokens** (e.g. malformed float literals).

### 4.2 Three approaches to building a lexer
1. **Lex-style tool** — write regular-expression-like patterns; a tool generates the analyzer (e.g. UNIX `lex`).
2. **State diagram + hand-written code** — design a state transition diagram, then code it directly.
3. **State diagram + table-driven** — same diagram, but implemented as a table.

### State diagrams for tokens
- A **state transition diagram** is a directed graph: nodes are states, arcs are labeled with input characters that trigger transitions, and may carry actions.
- These diagrams are **finite automata**. The set of token patterns of a language is a **regular language**.
- **Character classes** keep the diagram small: instead of 52 transitions for letters, define a class `LETTER`; instead of 10 for digits, a class `DIGIT`.

### Reserved words
Don't build a separate state subgraph for each reserved word. Recognize names with the same pattern as identifiers, then **look up** in a reserved-word table after the fact.

### Symbol table
The lexer typically *creates* symbol-table entries for user-defined names; later compiler phases fill in attributes (type, etc.).

### Worked example (Sebesta's `front.c`)
For the input
```
(sum + 47) / total
```
the analyzer produces:
```
( sum + 47 ) / total EOF
```
with token codes for left-paren, identifier, plus-op, int-literal, right-paren, div-op, identifier, EOF.

> **For your D1 §4.2** you must give a token category list (identifiers, literals, keywords, operators, separators) and a regex/pattern for each. Sebesta's "ShapeScript" example in the project handout shows the expected style:
> ```
> IDENT    = [a-zA-Z_][a-zA-Z0-9_]*
> INT_LIT  = [0-9]+
> FLOAT_LIT= [0-9]+ "." [0-9]+
> ```

## 4.3 The Parsing Problem

### 4.3.1 Goals of syntax analysis
1. **Detect** syntax errors and **recover** so as many as possible can be reported in one pass.
2. **Produce a parse tree** (or its trace) for syntactically correct input.

> **For your D2** the parser must reject malformed programs with **a useful error message including line number** (per handout §D2 [P1]).

### 4.3.2 Top-down parsers
- Build the parse tree **from root to leaves** (preorder), corresponding to a **leftmost derivation**.
- Decision: at each step, choose which RHS to expand the current leftmost nonterminal with, by examining the **next input token**.
- The most common top-down algorithms are **LL** parsers — first L = left-to-right scan, second L = leftmost derivation.
- **Recursive-descent** is the hand-coded form of an LL parser; table-driven LL is the alternative.

### 4.3.3 Bottom-up parsers
- Build the parse tree **from leaves to root**, corresponding to the **reverse of a rightmost derivation**.
- At each step, find the **handle** of the current right sentential form: the substring that must be reduced to its LHS to get the previous sentential form.
- The most common family is **LR** parsers (shift-reduce). LR parsers can handle a strictly larger class of grammars than LL.

### Complexity
- Parsers for arbitrary unambiguous grammars are **O(n³)**.
- Parsers used in real compilers work on subclasses (LL, LR) and run in **O(n)**.

### Notational conventions Sebesta uses (memorize for the exam)
- Lowercase letters from the start of the alphabet (`a, b, …`) — **terminals**.
- Uppercase letters from the start of the alphabet (`A, B, …`) — **nonterminals**.
- Uppercase letters from the end (`W, X, Y, Z`) — terminal *or* nonterminal.
- Lowercase letters from the end (`w, x, y, z`) — strings of terminals.
- Lowercase Greek (α, β, γ, δ) — mixed strings.

## 4.4 Recursive-Descent Parsing

### 4.4.1 The technique
- One **parsing subprogram per nonterminal**.
- The subprogram for nonterminal `N` traces out the parse tree rooted at `N` for whatever input it is given.
- EBNF is the natural input form for recursive descent: `{ X }` becomes a `while` loop, `[ X ]` becomes an `if`.
- Canonical example grammar:
  ```
  <expr>   → <term> { ( + | - ) <term> }
  <term>   → <factor> { ( * | / ) <factor> }
  <factor> → id | int_constant | ( <expr> )
  ```
- For each terminal, compare with the next input token; if matched, advance the lexer; if not, report a syntax error.
- For multiple RHSs, the subprogram first decides which RHS to use based on the next token (the **first set**).

### 4.4.2 The LL grammar class — restrictions
A recursive-descent parser works only on grammars that:
1. **Are not left-recursive** (direct or indirect). Direct left recursion `A → A + B` causes infinite recursion in the parser. Sebesta gives the standard transformation to eliminate it.
2. **Pass the pairwise disjointness test** — every parsing subprogram must be able to choose its RHS by looking at the next input token. Some grammars that fail can be fixed by **left factoring**.

> Implication for your project: write your EBNF so that each alternative starts with a distinct token (or is left-factored), and avoid left recursion. If you use a parser generator (ANTLR/yacc/bison/peg), you must still understand and explain these constraints in the exam.

## 4.5 Bottom-Up Parsing (overview)
- A bottom-up parser is a **shift-reduce** algorithm operating on a parse stack.
- **Shift** moves the next input token onto the stack.
- **Reduce** replaces an RHS (the handle) on top of the stack with its LHS.
- The mathematical model is a **pushdown automaton** (PDA).
- LR parsers use two tables: **ACTION** (what to do given the top state and next input token) and **GOTO** (which state to push after a reduction).
- LR parsers work on left-recursive grammars (so LR grammars often *look* nicer for arithmetic expressions than LL grammars).
- Building LR tables by hand is impractical for real languages; use a tool (yacc, etc.).

## Practical takeaways for the project
- Write the EBNF first (D1 §4.3), then derive the lexer's token list from the terminals you used (D1 §4.2), then write the recursive-descent parser following the EBNF subprogram-per-nonterminal pattern.
- **D1 ↔ D2 consistency is graded strictly** (handout §D2). If your D1 says "operator `+` is left-associative with lower precedence than `*`," your parser must produce the corresponding tree shape.
- Useful error messages include the *line number*. Track the line counter in the lexer (increment on every `\n`).

---

<a id="chapter-5"></a>
# Chapter 5 — Names, Bindings, and Scopes

This is the basis for **D1 §4.6 (Names, Binding, Scope, Lifetime)**. The exam is heavy on this chapter for Part 1, including questions like "show on this code excerpt how the lookup of variable x resolves under your declared scoping rule."

## 5.2 Names

### 5.2.1 Design issues
Two questions:
1. Are names **case sensitive**?
2. Are special words **reserved** or merely **keywords**?

### 5.2.2 Name forms
- A name is a string of characters identifying an entity.
- Typical form: a letter followed by letters/digits/underscores.
- C99: no length limit on internal names but only first 63 are significant; external names limited to 31 characters. Java/C# have no length limit and all characters are significant.
- Style: underscore vs. **camelCase** is a style choice, not a language design choice.
- Some languages mark variable types in the name itself: PHP `$name`; Perl `$scalar`, `@array`, `%hash`; Ruby `@instance`, `@@class`.

### 5.2.3 Special words: reserved words vs. keywords
- **Reserved word** — cannot be used as a name. (Most modern languages.)
- **Keyword** — special word that can also be redefined as an identifier. (Fortran allows `Do` and `End` as variable names.) Hurts readability.
- **Predefined names** — defined in libraries; visible only when imported; once imported, cannot be redefined.

> COBOL has 300 reserved words including `LENGTH`, `BOTTOM`, `DESTINATION`, `COUNT` — many of which programmers naturally want as variable names. This is the canonical "too many reserved words" warning.

## 5.3 Variables — the sextuple of attributes
A variable is an abstraction of a memory cell, characterized by:

| Attribute | Meaning |
|-----------|---------|
| **Name** | the identifier (§5.2). |
| **Address** | machine memory address. Also called the **l-value** (the side of an assignment that needs an address). One variable can have different addresses at different times (e.g. each call to a function gives its locals new addresses). |
| **Type** | range of values + set of operations. |
| **Value** | the contents of the memory cell. Also called the **r-value** (the side that needs a value). To get the r-value, the l-value must be determined first. |
| **Lifetime** | time during which the variable is bound to a memory cell (§5.4.3). |
| **Scope** | range of statements in which the variable is visible (§5.5). |

### 5.3.2 Aliases
**Aliases** are two or more variable names bound to the same address. Aliases hurt readability and verifiability. Sources of aliasing in real languages: union types, two pointers/references to the same object, subprogram parameters (Ch. 9).

## 5.4 The Concept of Binding

### 5.4 Binding times
A **binding** is an association between an attribute and an entity. **Binding time** is when the binding happens. Possible binding times:
- **Language design time** — the meaning of `*` for multiplication.
- **Language implementation time** — the range of values for `int` in C.
- **Compile time** — the type of a Java variable.
- **Load time** — a variable bound to a storage cell.
- **Link time** — a library subprogram call bound to its code.
- **Run time** — the value of a variable in a particular execution.

### 5.4.1 Static vs. dynamic
- **Static binding** — first occurs before run time and does not change during execution.
- **Dynamic binding** — first occurs at run time, or can change during execution.

### 5.4.2 Type bindings
Two questions: how is the type specified, and when does the binding happen.

#### 5.4.2.1 Static type binding
Type is bound before run time and does not change.
- **Explicit declaration** — a statement listing names and their type. (C, Java, C#, Swift in default modes.)
- **Implicit declaration** — first appearance of a name implicitly declares it. Hurts reliability — typos slip past the compiler. Variants: (a) by *naming convention* (Perl `$`/`@`/`%`); (b) by *type inference* (C# `var sum = 0;` — the type is fixed for the lifetime).

#### 5.4.2.2 Dynamic type binding
The variable is bound to a type when it is *assigned* a value. Type can change between assignments. Used by Python, JavaScript, Ruby, PHP. Also opt-in in C# (the `dynamic` keyword).

**Trade-offs of dynamic typing** (Sebesta states all of these explicitly):
- ✅ Greater programming flexibility (generic code without templates).
- ❌ Less reliability — typos like `i = y` (with `y` an array) silently change `i`'s type instead of being a compile-time error.
- ❌ Higher cost — type checking at run time, every variable carries a run-time type descriptor, storage size varies.
- ❌ Usually requires pure interpretation (≥10× slower than compiled equivalents).

### 5.4.3 Storage Bindings and Lifetime — four categories of scalar variables
The **lifetime** of a variable is the time during which it is bound to a specific memory cell. Sebesta groups *scalar* variables into four categories by lifetime:

#### 5.4.3.1 Static variables
- Bound to a memory cell **before program execution begins** and stay there until termination.
- ✅ Direct addressing → fast. No allocation/deallocation overhead.
- ✅ Useful for globals and history-sensitive subprograms.
- ❌ Cannot support recursion (no per-activation locals).
- ❌ Cannot share storage between subprograms not active simultaneously.
- C/C++: `static` in a function.

#### 5.4.3.2 Stack-dynamic variables
- Storage is bound when the **declaration is elaborated** (executed); type is bound statically.
- Allocated on the **run-time stack**.
- ✅ Required for recursion (each activation has its own copy).
- ✅ Different subprograms share the same memory space for their locals.
- ❌ Run-time overhead for allocation/deallocation, indirect addressing.
- ❌ Subprograms cannot be history-sensitive.
- Java/C#/C++: locals in methods/functions.

#### 5.4.3.3 Explicit heap-dynamic variables
- **Nameless** memory cells allocated/deallocated by **explicit programmer instructions** (`new`/`delete` in C++, `malloc`/`free` in C). Accessed only via pointers/references.
- ✅ Build dynamic structures (linked lists, trees).
- ❌ Pointer/reference correctness is hard; heap management is costly and complex.
- C++ has `new`/`delete`; Java objects are explicitly heap-dynamic but garbage-collected (no `delete`).

#### 5.4.3.4 Implicit heap-dynamic variables
- All attributes — including type and storage — are bound **every time** the variable is assigned. JavaScript: `highs = [74, 84, 86, 90, 71];` — the variable becomes an array of five numbers regardless of its previous role.
- ✅ Maximum flexibility.
- ❌ Run-time overhead for maintaining all dynamic attributes; lost compiler error-detection.

> **For your D1 §4.6** you must say which of these four lifetimes your local variables have. Stack-dynamic is the standard "interpreter-friendly" choice for a small DSL.

## 5.5 Scope

### Definition
The **scope** of a variable is the range of statements in which it is visible. **Local** variables are declared within the unit/block; **nonlocal** variables are visible but not declared there; **globals** (§5.5.4) are a special category of nonlocals.

### 5.5.1 Static (lexical) scope
Introduced by ALGOL 60. Scope is determined **before execution**, by the *textual* (lexical) structure of the program. To resolve a reference to `x` in subprogram `sub1`:
1. Search `sub1`'s declarations.
2. If not found, search the **static parent** (the unit that *declared* `sub1`).
3. Continue up the **static ancestors** until found, or until the outermost unit fails.
4. If never found → undeclared variable error.

A declaration of `x` in an inner scope **hides** any outer declaration of the same name.

There are two flavors of static-scoped languages:
- Those allowing **nested subprograms** (Ada, JavaScript, Common Lisp, Scheme, Fortran 2003+, F#, Python).
- Those that do not (the **C family**); nesting comes only from blocks and class definitions.

### 5.5.2 Blocks
A **block** is a section of code (e.g. any C compound statement `{ … }`) that opens its own scope. Variables declared in a block are typically stack-dynamic — allocated on entry, deallocated on exit. ALGOL 60 introduced blocks; the term "block-structured language" comes from this.

> Java/C# disallow re-using a name in a nested block (the designers thought it was too error-prone). C/C++ allow it.

Functional languages use a **`let` construct** (Scheme `LET`, ML `let … in … end`) for the same purpose: binding names locally to expressions.

### 5.5.4 Global scope
A special kind of nonlocal. Sebesta discusses how Python handles `global` and `nonlocal` declarations explicitly to control whether an assignment creates a new local or modifies the enclosing variable.

### 5.5.5 Evaluation of static scoping
- Allows more access than is needed; awkward when programs evolve and structure changes; pushes designers toward unnecessary globals.
- Encapsulation constructs (Ch. 11) are an alternative.

### 5.5.6 Dynamic scope
- The scope of a variable is determined by the **calling sequence** of subprograms, not by their textual nesting.
- To resolve a name, search the **local declarations** first, then the declarations of the **dynamic parent** (the *calling* function), then *its* dynamic parent, and so on, until a declaration is found — or a run-time error.
- Used historically in APL, SNOBOL4, and early Lisp; opt-in in Perl and Common Lisp.

### Example (Sebesta §5.5.1 / §5.5.6)
```js
function big() {
  function sub1() { var x = 7; sub2(); }
  function sub2() { var y = x; }   // which x?
  var x = 3;
  sub1();
}
```
- **Static scoping**: `x` in `sub2` resolves to `big`'s `x = 3` (because `sub2`'s static parent is `big`). The `x` in `sub1` is irrelevant.
- **Dynamic scoping**: depends on the call chain. If `big → sub1 → sub2`, then `sub2`'s search reaches `sub1` first, finds `x = 7`. If `big` calls `sub2` directly, `x` resolves to `big`'s `x = 3`.

### 5.5.7 Evaluation of dynamic scoping
- ❌ Type checks on nonlocal references cannot be done statically.
- ❌ Local variables are visible to *any* called subprogram while active — no protection.
- ❌ Slower nonlocal access than static.
- ✅ Slightly less parameter passing.
- Almost every modern language uses static scoping for these reasons.

> **For your D1 §4.6** the handout *requires* you to answer: "static or dynamic, and why; what would break if you switched?" Static is the safe and expected default for almost any DSL — "what would break if you switched" is essentially the §5.5.7 list.

## 5.6 Scope and Lifetime
**These are not the same thing.** Scope is *spatial/textual*; lifetime is *temporal*.

Two examples Sebesta gives that show the dissociation:
- A C `static` local variable inside a function: scope is local to the function, lifetime is the entire program.
- A variable `sum` in a function `compute()` that calls `printheader()`: `sum`'s scope does not include `printheader`'s body, but `sum`'s lifetime *does* extend over the time `printheader` is running (because `compute` is suspended, not terminated).

## 5.7 Referencing Environments
The **referencing environment** of a statement is the collection of all variables visible to that statement.
- **Static-scoped**: locals + visible variables of static ancestors.
- **Dynamic-scoped**: locals + variables of all currently active subprograms (with hiding by more-recent activations).

> Useful exam tactic: when asked "what variables are in scope at point X?", first state which scoping rule applies, then list the locals, then walk the (static or dynamic) ancestor chain.

## 5.8 Named Constants
A named constant is a variable bound to a value **only once**. Improves readability (use `pi` not `3.14159265`) and reliability.

---

<a id="project-mapping"></a>
# Project Mapping (Sebesta → D1 sections)

This is the cross-reference table you should use when writing each section of D1 to make sure every claim cites the right Sebesta chapter and is using the book's own vocabulary (which the rubric §5 grades on under "Use of Sebesta Framework").

| D1 Section | Required content | Sebesta sections to cite |
|---|---|---|
| **§4.1 Language Overview** [P1] | Name, domain, sample program, one-paragraph justification using the four evaluation criteria. State which your DSL prioritizes and which it sacrifices. | **§1.2** (programming domains), **§1.3** (readability/writability/reliability/cost), **§1.6** (trade-offs) |
| **§4.2 Lexical Structure** [P1] | Token categories: identifiers, literals, keywords, operators, separators. Regex/pattern for each. | **§3.2** (lexemes vs. tokens), **§4.2** (lexical analyzer, state diagrams, regular languages) |
| **§4.3 Syntax** [P1] | Complete EBNF grammar; unambiguous; annotate ambiguity-resolution decisions (precedence, associativity, dangling else). | **§3.3.1** (BNF / context-free grammars), **§3.3.1.7** (ambiguity), **§3.3.1.8** (precedence), **§3.3.2** (EBNF). Also **§4.4.2** (LL constraints — left recursion, pairwise disjointness — *if* using recursive descent). |
| **§4.6 Names, Binding, Scope, Lifetime** [P1] | Legal identifiers; what binds at compile time vs. run time; static or dynamic scoping (and why; what would break if switched); lifetime category (static / stack-dynamic / explicit heap-dynamic). | **§5.2** (identifier rules, reserved vs. keyword), **§5.3** (sextuple of attributes), **§5.4** (binding & binding times — including 5.4.2 on type binding and 5.4.3 on the four lifetime categories), **§5.5** (scope: static vs. dynamic, blocks), **§5.5.7** (why static wins), **§5.6** (scope vs. lifetime distinction), **§5.7** (referencing environments) |

---

# Quick Vocabulary Cheat-Sheet
Terms the grader and exam will expect to see used precisely:

- **lexeme** vs. **token**
- **terminal** vs. **nonterminal**
- **grammar / production / rule / RHS / LHS**
- **derivation (leftmost / rightmost) / sentential form / sentence**
- **parse tree / ambiguity**
- **EBNF — `[ ]`, `{ }`, `( | )`**
- **left-recursive / pairwise disjointness / left factoring** (Ch. 4)
- **recursive descent / LL / LR / shift-reduce / handle**
- **regular grammar / finite automaton / character class**
- **static semantics** (Ch. 3) vs. **dynamic semantics**
- **operational / denotational / axiomatic** semantics
- **name / address / l-value / r-value / type / value / lifetime / scope** (the sextuple)
- **alias**
- **binding / binding time / static binding / dynamic binding**
- **explicit declaration / implicit declaration / type inference**
- **static / stack-dynamic / explicit heap-dynamic / implicit heap-dynamic** (the four lifetimes)
- **static (lexical) scope / dynamic scope**
- **static parent / static ancestor / dynamic parent**
- **block / `let` construct**
- **referencing environment**
- **named constant**
- **readability / writability / reliability / cost** (the four primary criteria)
- **simplicity / orthogonality / data types / syntax design / expressivity / type checking / exception handling / restricted aliasing** (the characteristics in Sebesta's Table 1.1)

---

*End of guide. If a topic appears in this file but the agent is unsure of a detail, it must search the book before answering — the book is the source of truth.*
