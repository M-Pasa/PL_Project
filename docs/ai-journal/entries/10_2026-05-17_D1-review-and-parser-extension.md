---
entry: 10
date: 2026-05-17
phase: Design review (§4.4/§4.5/§4.7/§4.8) + Parser extension to full §4.7 expression layer
ai_tool: Claude Opus 4.7
---

# Entry 10 — D1 P2 review fixes + §4.7 expression-layer parser extension

## Goal

Two pieces of work that ran in the same session, before E2 and E3 (entries 08 and 09):

1. Critically re-read the P2 design sections of `docs/design/D1.md` drafted in entry 07 (§4.4 semantics, §4.5 type system, §4.7 expressions, §4.8 rationale) and fix anything I could not defend in the quiz.
2. Extend the lexer/parser/AST from the P1 expression form (`<expression> → <term> { ("+"|"-") <term> }`) to the full P2 §4.7.1 precedence-stratified grammar, so the type checker (entry 09) had something non-trivial to type-check.

## Prompt

Same conversational session as entries 08–09. For step 1, I asked Claude to walk through the four P2 sections and flag anything I should re-examine before locking the draft. For step 2, I asked Claude to extend the parser to the §4.7 grammar.

## Response — D1 P2 review

Claude read §4.4 / §4.5 / §4.7 / §4.8 and surfaced **one trivial cleanup**, **one real internal inconsistency**, and **four defendability flags I should think about but no code change for**:

### Cleanup

- Two `## 4.8` headers existed (lines 738 and 764). The second was a leftover P1 stub. Removed.

### Real internal inconsistency

- §4.5.5 says "*No other rate arithmetic is allowed — no rate + rate.*" But the §4.5.8 type-checker pseudocode for `+`/`−` was `check(e1) = check(e2) ⇒ check(e1)` — which would silently accept `Rate(D,T) + Rate(D,T)`. Fix: tightened the pseudocode to enumerate the two legal cases (`Number+Number`, `Quantity D + Quantity D`) so Rate+Rate now falls through to the error branch. Bug found by reading the spec back against itself — exactly the failure mode of writing two adjacent subsections in different sittings.

### Minor formalism gap

- §4.4.5 uses `⇝` for the environment-extension judgement, but §4.4.1's "Style and meta-language" subsection introduces only `⇓`. Added one sentence in §4.4.1: *"Declarations use a separate judgement `env ⊢ decl ⇝ env'`, read 'in environment env, declaration decl extends the environment to env'' — used by the (LET) rule of §4.4.5."*

### Defendability flags (no code change, but I should be ready to answer)

- **§4.7.1 LL(1) claim** — the rate-vs-divide disambiguation is described as one-token-past-`/` peek. Strictly that is k=2 lookahead from the `/` token, even though it is one-symbol-after-the-operator. If asked "is your grammar strictly LL(1)?", the honest answer is "LL(1) with one token of post-operator peek inside `parseAtom`."
- **§4.4.9 `per UNIT of bodyweight` sugar** — defended by analogy to Ada `'Pos`. The analogy is loose (`'Pos` is enum→int, here it is `Mass`→`Number`). Better framing for the quiz: *"a single named projection from a typed value to a number, available only inside the named sugar form — never as a general expression operator."* That preserves the closed type universe of §4.5.1.
- **§4.5.4 rule 1 example** uses `1 lb + 1 kg` — needs `lb` to actually be a unit token. Verified: `LB` is in `Token.Type` and `Ast.familyOf` maps it to MASS. Example stands.
- **§4.4.7 routine scoping** — wording implies routines see their declaration-site env, distinct from caller-site. Given the flat athlete-scope env + no-forward-reference rule, this collapses to "sees every let textually before the routine declaration." Not buying any expressivity beyond what the flat env already gives.

## Response — Parser extension to §4.7

### Grammar implemented

```ebnf
<expression>   → <add-expr>
<add-expr>     → <mul-expr> { ("+" | "-") <mul-expr> }
<mul-expr>     → <unary>    { ("*" | "/") <unary>    }
<unary>        → [ "-" ] <atom>
<atom>         → NUM_LIT [ UNIT [ "/" UNIT ] ]      // scalar | quantity | rate
               | IDENT
               | "(" <expression> ")"
```

### AST additions (`src/fitlang/ast/Ast.java`)

```java
public static final class UnaryExpr implements Expr {
    public final String op;          // "-"
    public final Expr   operand;
}
public static final class RateExpr implements Expr {
    public final Rate rate;
}
```

`BinaryExpr.op` was already `String`, so `*` and `/` needed no AST change.

### The rate-vs-divide disambiguation

The interesting decision is where to disambiguate `NUM_LIT UNIT "/" UNIT` (a rate atom) from `NUM_LIT UNIT "/" <expr>` (a multiplicative divide whose left operand is a quantity). Two reasonable strategies:

(a) Disambiguate at `<mul-expr>` level — peek when we see `/`.
(b) Disambiguate inside `parseAtom` — after consuming `NUM_LIT UNIT`, peek two tokens; if they are `SLASH` then `UNIT`, consume both and produce a `RateExpr`. Otherwise return a bare `QuantityExpr` and let the caller's `<mul-expr>` handle the slash.

Claude went with (b). It keeps `parseMulExpr` clean (no special case for "left operand was a quantity"), and it puts the lookahead at the point where we have all the information to decide (we have just consumed the `UNIT` that distinguishes "quantity-followed-by-something" from "bare-number-followed-by-something"). The k=2 lookahead lives in one method, not threaded through the recursive descent.

### Smoke test

After the parser change, a synthetic `_p2_expr_smoke.fl` exercised every new form:

```fitlang
let topSet     = 80 kg
let half       = 80 kg / 2             # binary divide (Quantity / Number)
let doubled    = 2 * 40 kg             # binary multiply
let combined   = (80 kg + 200 g) - 5 kg # parens + same-family additive
let bulkRate   = 0.25 kg/week          # rate atom
let negRate    = -0.25 kg/week         # unary over rate atom
let projected  = 0.25 kg/week * 4 week # rate × time (Rate(Mass,Time) × Quantity(Time))
```

`--dump-ast` produced exactly the precedence-correct tree shape — `combined` printed as `((80.0 kg + 200.0 g) - 5.0 kg)` rather than `(80.0 kg + (200.0 g - 5.0 kg))`, confirming left-associative `+`/`-` at the `<add-expr>` level. The three existing valid samples still parsed identically (no regression on the P1 subset).

The smoke file was later removed when its job — proving the new forms parsed — was done; the same expressions reappear inside `samples/valid_*.fl` and `samples/invalid_typeerror.fl`.

## Accepted

- All three D1 fixes: duplicate §4.8 removed, §4.5.8 pseudocode tightened, `⇝` introduced in §4.4.1.
- The parser extension as Claude wrote it, including strategy (b) for rate-vs-divide. Compiled clean, all P1 samples regression-passed, all new expression forms produced precedence-correct ASTs.

## Rejected / modified

- Nothing in this entry's scope was rejected. The four defendability flags (LL(1), `'Pos` analogy, `lb` example, routine scoping) were left as defence-prep, not as code or spec changes — they are correctly written, I just need to be ready to answer.

## Errors I caught

- The `Rate+Rate` pseudocode bug in §4.5.8 is the headline. It is the kind of bug that only appears when you read a long spec back against itself — two sections written hours apart can each be internally consistent yet contradict each other. Sebesta §3.4 warns about exactly this when ambiguity is split across non-adjacent rules. Without the review pass this bug would have shipped — the existing samples never exercised `Rate+Rate` and the type checker (entry 09) would have implemented the lenient pseudocode, accepting nonsense like `0.25 kg/week + 2.5 kg/week` silently.
- Claude correctly stopped before §4.7.1's LL(1)-vs-k=2 framing and left it as a defendability flag rather than rewriting the spec to say "k=2". The honest framing belongs in my head for the quiz, not necessarily in the prose.

## Reflection

The Rate+Rate bug would have shipped silently if I didnt do the review pass, two sections written hours apart can each look fine but contradict each other. Parser extension was mostly mechanical, the only real call was the k=2 peek inside parseAtom and Claude picked the cleaner spot for it.
