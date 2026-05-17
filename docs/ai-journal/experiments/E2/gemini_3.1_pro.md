Date: 17.05.2026
Model: Gemini 3.1 pro
session link: https://gemini.google.com/share/4b680ecb5347

Prompt:  I'm designing a small DSL called FitLang for writing fitness training programs together with nutrition. It has four record-like declarations, each
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
FitLang’s highly constrained, schema-driven design makes the choice between name and structural equivalence relatively straightforward. Because the language restricts users from defining custom records or adding fields, the type system exists primarily to enforce domain logic rather than to provide general-purpose data modeling.

Here is a breakdown of how both options apply to FitLang through the lens of Robert W. Sebesta’s *Concepts of Programming Languages*, along with the impact on your type checker and a final recommendation.

### **Structural Equivalence**

In structural equivalence, two variables have equivalent types if their internal structures are identical. For record kinds, this means the type checker looks at the field names, the types of those fields, and their order (though some languages relax the ordering rule).

* **What it buys:** Supreme flexibility. It allows a developer to pass any object to a function as long as it has the required fields. It is highly suited for dynamic or heavily composable environments.
* **What it costs:** Accidental equivalence. If two concepts happen to share the same data footprint, the compiler allows mixing them up. It also places a heavier computational burden on the compiler.
* **Real-world examples:** TypeScript (duck typing/structural subtyping), Go (for interfaces), OCaml, and Elm.
* **Impact on the FitLang Type Checker:** To implement this, your type checker must perform a recursive comparison of AST nodes. When checking if a record is valid for a specific context, the checker must extract the list of fields, verify that every expected field name exists, and check that the types (like `N` or `Mass`) match perfectly.

### **Name Equivalence**

In name equivalence, two variables have equivalent types only if they are defined in the same declaration or use the same type name. For FitLang, the "type name" would simply be the fixed keywords: `exercise`, `meal`, `workout`, and `routine`.

* **What it buys:** Semantic safety and strict domain enforcement. It prevents logically distinct but physically similar data structures from being conflated. It is highly intentional; you cannot accidentally pass a `meal` to a `routine`.
* **What it costs:** Rigidity. Developers often have to write boilerplate code to convert between structurally identical types just because they have different names.
* **Real-world examples:** C (for structs), C++, Java, Rust, and Ada.
* **Impact on the FitLang Type Checker:** Implementation is trivial. The type checker only needs to check the AST node's tag. If a block expects an `exercise`, the checker verifies `node.type == TYPE_EXERCISE`. It is a simple, constant-time integer or string comparison.

### **Recommendation for FitLang**

FitLang should use **Name Equivalence**, treating the four fixed keywords as nominal types.

Structural equivalence solves a problem FitLang simply does not have. Because users cannot define custom schemas, they will never create a bespoke record that needs to be structurally pattern-matched against an `exercise`.

Furthermore, your domain demands semantic boundaries. Even if a future update to FitLang resulted in two distinct kinds having the same fields (for example, if a `meal` was updated to just track a single `calorie: N` field, and a new `cardio` kind tracked a single `duration: N` field), they are fundamentally different concepts in the real world. A fitness coach would never want an engine that allows substituting a meal for a treadmill session just because they both happen to hold a single integer. Name equivalence guarantees that the domain logic remains pure.

By mapping `exercise`, `meal`, `workout`, and `routine` as strict nominal types, your type checker remains fast and simple, your error messages become incredibly clear ("Expected an exercise, got a meal"), and the language enforces the fitness domain strictly.