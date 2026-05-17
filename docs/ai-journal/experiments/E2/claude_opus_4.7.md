Date: 17.05.2026
Model: Claude Opus 4.7
session link: https://claude.ai/share/b913ab0f-efa4-4de4-bc03-3c71257ebc62

Prompt:
 I'm designing a small DSL called FitLang for writing fitness training programs together with nutrition. It has four record-like declarations, each
   introduced by a fixed keyword:
  
   - exercise "L" { sets: N, reps: N, weight: Mass }
   - meal "L" { protein: Mass, carbs: Mass, fat: Mass } (one or more of these fields)
   - workout "L" { ... list of exercises ..., optional progress rate }
   - routine "L" (p: UNIT, ...) { ... list of exercises ... }
  
   The grammar admits exactly one schema per kind keyword — a user cannot define their own record kinds and cannot add fields. Mass is a dimensional
   quantity type (kg, g, lb — all the same type).
  
   Question: should FitLang use name equivalence or structural equivalence for these record kinds? Walk me through both options in the Sebesta Concepts of
   Programming Languages sense (Ch. 6), what each buys and costs, which real languages take each path, and which you'd pick for FitLang. Be specific about
   how the choice affects the type checker.

Output:
Good question — and it's worth being precise about what "name" vs. "structural" equivalence even means in Sebesta's framework before applying it to FitLang.

---

## What Sebesta means by each

**Name equivalence** says two types are compatible if and only if they refer to the *same type declaration* — either strictly (the exact same declaration site, "declaration equivalence") or loosely (declared with the same name, "declaration name equivalence"). In Sebesta's strict form, even two separately declared `type Point = {x: int, y: int}` in Ada would be distinct.

**Structural equivalence** says two types are compatible if their structures are recursively identical — same field names, same field types, same order (in some formulations). The type checker walks the tree; the name of the type declaration is irrelevant.

---

## Applying both to FitLang

FitLang has four kinds, each with a *fixed, closed schema*. So the question concretely is: if you write a function or operation that expects an `exercise`, can a value whose fields happen to match `exercise`'s schema satisfy it?

### Structural equivalence in FitLang

The type checker compares field sets. Since `workout` contains a list of exercises and a progress rate, and `exercise` has `{sets, reps, weight}`, the checker has to walk nested structures on every compatibility check.

**What it buys:**
- Nothing useful here. FitLang has no user-defined record kinds, so there is never a situation where two *independently written* records happen to be structurally the same and you'd *want* them interchangeable. The only structural matches would be accidental.

**What it costs:**
- `meal "Breakfast" { protein: 30g, carbs: 0g, fat: 10g }` and a hypothetical coincidentally-shaped record would be considered the same type. This is a semantic disaster — a meal is not an exercise, even if they had the same fields.
- The type checker has to do tree-walking equality on every assignment or call site. More expensive and more complex to implement.
- Error messages become confusing: *"expected exercise, got a record with fields sets: int, reps: int, weight: Mass"* — you've lost the kind name in the error.

Real languages that take this path: **TypeScript** (structural for interfaces), **OCaml** (row polymorphism for objects, though not for records by default), **Go** (interfaces are structural).

---

### Name equivalence in FitLang

The type checker tags each value with exactly which keyword introduced it — `exercise`, `meal`, `workout`, or `routine`. Compatibility is a tag comparison: an `exercise` is only an `exercise`.

**What it buys:**
- `exercise` and `meal` can never be confused even if their field types happened to overlap. A `meal` with `{protein: Mass, carbs: Mass, fat: Mass}` and an `exercise` with `{sets: int, reps: int, weight: Mass}` are distinct by construction — but more importantly, *if you later add a DSL operation that accepts a "nutritional item,"* you can gate it on the `meal` tag, not on field shapes.
- Type errors are maximally clear: *"expected exercise, got meal."*
- The type checker is trivial to implement: tag lookup and equality. O(1) per check.
- It enforces the *semantic intent* of each kind. FitLang's kinds are domain concepts, not anonymous data bags. A `workout` is not just a list — it's a training unit with specific semantics about how progress rate applies.

**What it costs:**
- Technically, you cannot write a generic "thing with a weight field" operation without special-casing it. But FitLang has no generics and no user-defined combinators, so this cost is essentially zero.
- If you ever extend the language to allow subtyping (e.g., `powerlifting_exercise` extends `exercise`), you'd need to layer declaration equivalence or an explicit subtype relation on top. But again, the spec says no user-defined kinds, so this is moot.

Real languages that take this path: **Ada** (strict name equivalence for types), **Pascal** (declaration equivalence for records), **Haskell** (`newtype` and named ADTs are name-equivalent even if structurally identical inside).

---

## The type-checker impact, concretely

With **structural equivalence**, your checker needs a `structurallyEqual(T1, T2)` function that recurses into field lists, handles `Mass` as a unified type (so `kg`, `g`, `lb` are the same), and compares field-by-field. Every assignment, every list element addition (adding an exercise to a workout's list), every parameter pass triggers this walk.

With **name equivalence**, your checker just carries a `KindTag ∈ {exercise, meal, workout, routine}` on every value. A type environment maps each declared label to its kind. Checking `addToWorkout(w, e)` is: *is w tagged `workout`? Is e tagged `exercise`?* Two pointer comparisons. You do structural checking exactly once — when you *parse* a declaration, to verify all required fields are present and have the right types. After that, structure is irrelevant to the type system.

---

## Which to pick for FitLang

**Name equivalence, clearly.** The reasons stack up:

1. The four kinds are *domain-semantic*, not just structurally convenient. Conflating a `meal` with an `exercise` would be a user error, not a feature.
2. There are exactly four fixed kinds — the closed world means name equivalence has zero expressiveness cost.
3. The `Mass` type already does the right structural work at the field level (unifying `kg`, `g`, `lb`), so structural equivalence isn't needed to handle the one genuinely structural thing in the language.
4. Type checker implementation is dramatically simpler and error messages are dramatically better.

The only footnote: for `Mass` itself — the dimensional type — you do want *structural/unit equivalence* (30 kg == 30000 g in type-compatible terms), but that's a value-level unit conversion question, not a record kind question. The kinds themselves should be name-equivalent.