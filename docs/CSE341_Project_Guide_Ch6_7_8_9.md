# CSE 341 — DSL Project Guide (Sebesta Ch. 6, 7, 8, 9)

> **Purpose of this document.** Companion to the Part 1 guide. This reference covers Sebesta's *Concepts of Programming Languages* (12th ed.), Chapters **6, 7, 8, 9**, which are the chapters the **Part 2** material is built on: data types, expressions/assignment, statement-level control structures, and subprograms. These four chapters are the primary source for D2's interpreter design (how your DSL's expressions, statements, and functions actually behave) and for the D1 §4.4 (Semantics) and §4.5/§4.7 (Control & Subprograms) sections in the full deliverable.
>
> **Source of truth: the book.** Every concept, term, and rule below is from Sebesta. Do not invent semantics that are not in the book. When in doubt, search the book first.
>
> **How to use this file.** Each top-level section maps to one Sebesta chapter. Inside each chapter, subsections follow the book's section numbering so the agent can cite §6.5, §7.2, §8.3, §9.5, etc. directly in the document and exam answers. The final section ("Project Mapping") shows which book section each Part 2 deliverable topic should cite.

---

## Table of Contents

1. [Chapter 6 — Data Types](#chapter-6)
2. [Chapter 7 — Expressions and Assignment Statements](#chapter-7)
3. [Chapter 8 — Statement-Level Control Structures](#chapter-8)
4. [Chapter 9 — Subprograms](#chapter-9)
5. [Project Mapping (Sebesta → Part 2 sections)](#project-mapping)
6. [Quick Vocabulary Cheat-Sheet](#cheatsheet)

---

<a id="chapter-6"></a>
# Chapter 6 — Data Types

This chapter is the source for any section of the D-document that lists the DSL's **types and the operations defined on them**. Definitions, design issues, and implementation notes for each kind of type come from here.

## 6.1 Introduction
- A **data type** defines a *collection of values* and a *set of predefined operations* on those values.
- A **descriptor** is the collection of the attributes of a variable. It is an area of memory storing the variable's attributes; if all attributes are static, the descriptor exists only at compile time (usually inside the symbol table), otherwise part or all of it is maintained at run time.
- **Object** is reserved in Sebesta for instances of user-defined and language-defined abstract data types, not for values of predefined-type variables (Chapters 11–12).
- Structured types are defined with **type operators / constructors** (e.g. C's `[]` and `*`).

## 6.2 Primitive Data Types
**Primitive types** are types not defined in terms of other types.

### 6.2.1 Numeric Types
- **6.2.1.1 Integer.** Most common primitive numeric type, usually directly hardware-supported. Java has four signed sizes: `byte`, `short`, `int`, `long`. C++ and C# also have unsigned types. Python's `long` integer is not hardware-supported and has unlimited length. Negative integers are usually stored in **twos-complement**; **ones-complement** has two representations of zero.
- **6.2.1.2 Floating-Point.** Models real numbers — only approximations. Most modern machines use **IEEE Floating-Point Standard 754** (single = 8-bit exponent + 23-bit fraction, double = 11 + 52). Defined by **precision** (bits in the fraction) and **range** (range of exponents). Even simple values like 0.1 (decimal) are not exactly representable in binary.
- **6.2.1.3 Complex.** Supported in Fortran and Python; pair of floats. Python literal: `(7 + 3j)`.
- **6.2.1.4 Decimal.** Stores a fixed number of decimal digits with implied decimal point. Essential to COBOL; also in C# and F#. Represented as **binary coded decimal (BCD)**. Advantage: exact storage for decimals like 0.1. Disadvantages: no exponents (limited range) and wasteful representation.

### 6.2.2 Boolean Types
Simplest type: two values (true, false). Introduced in ALGOL 60. C89 has no Boolean type — numeric expressions are used (0 = false, nonzero = true). C99 and C++ have a Boolean type but also still allow numerics. Java and C# do not. Often stored in a byte even though one bit would suffice.

### 6.2.3 Character Types
Characters stored as numeric codings.
- **ASCII** — 8-bit code, values 0–127 (128 characters).
- **ISO 8859-1** — 8-bit, 256 characters.
- **Unicode / UCS-2** — 16-bit, published 1991 by the Unicode Consortium; first 128 characters are identical to ASCII. Used by Java (first widely used), JavaScript, Python, Perl, C#, F#, Swift.
- **UCS-4 / UTF-32** — 4-byte code, ISO/IEC 10646.
- Python has no separate char type — single characters are strings of length 1.

## 6.3 Character String Types
### 6.3.1 Design Issues
- Should strings be a special array of characters, or a primitive type?
- Should strings have static or dynamic length?

### 6.3.2 Strings and Their Operations
Most common operations: assignment, **catenation**, **substring reference**, comparison, **pattern matching**.
- A substring reference is a special case of the more general array **slice**.
- C/C++ store strings as `char` arrays terminated by `null` (zero). Library functions: `strcpy`, `strcat`, `strcmp`, `strlen`. These are **inherently unsafe** — they do not check destination length, leading to buffer overflows.
- Java has `String` (immutable) and `StringBuffer` (changeable).
- Python strings are an immutable primitive type with subscripting, catenation, `in`, and methods.
- F# strings are class objects, UTF-16, immutable, catenated with `+`.
- ML strings are primitive, immutable, catenated with `^`.
- Perl/JavaScript/Ruby/PHP support pattern matching directly via **regular expressions** (descended from UNIX `ed`). Example: `/[A-Za-z][A-Za-z\d]+/` matches typical identifier names; `/\d+\.?\d*|\.\d+/` matches numeric literals. C++/Java/Python/C#/F# provide regex via libraries.

### 6.3.3 String Length Options
- **Static length** — set when string is created. Used by Python, Java's `String`, Ruby's built-in `String`, .NET (C#/F#).
- **Limited dynamic length** — variable up to a declared maximum. Used by C and C-style strings in C++. The end is marked by `null`.
- **Dynamic length** — no maximum. Used by JavaScript, Perl, C++ standard library. Most flexible, but requires dynamic storage management.

### 6.3.4 Evaluation
String types greatly aid writability. Adding strings as a primitive is cheap; therefore it is hard to justify omitting them.

### 6.3.5 Implementation of Character String Types
- **Static** strings need a compile-time descriptor with: name, length, address.
- **Limited dynamic** strings need a run-time descriptor: max length, current length, address. C/C++ omit this because of the null terminator (and because subscripts are not range-checked).
- **Dynamic** strings need only current length + address but require complex storage management. Three implementation strategies:
  1. Linked list of characters (extra link storage, complex ops).
  2. Array of pointers to characters allocated on the heap.
  3. Complete strings in adjacent cells, moved when they grow (most common in practice).

## 6.4 Enumeration Types
All possible values, called **enumeration constants**, are named in the type definition. Typical example: `enum days {Mon, Tue, ..., Sun};`.

### 6.4.1 Design Issues
- Can an enumeration constant appear in more than one type?
- Are enum values coerced to integer?
- Are other types coerced to the enum type?

### 6.4.2 Designs
- In **C++**, enum constants default to 0,1,…; constants are coerced to int when needed; numeric values cannot be assigned to enum variables without a cast; enum constants can appear in only one type in the same scope.
- **Java 5.0** enums are implicit subclasses of `Enum`; they can have fields, constructors, methods; no expression of any other type can be assigned to an enum variable; enums are never coerced.
- **C#** enums are like C++'s but are *never* coerced to integer.
- **ML** defines enums as `datatype weekdays = Monday | Tuesday | ...`. **F#** uses `type` with `|`.
- **Swift** enums are pure named values (no internal integer); referenced as `fruit.apple`.
- None of Perl, JavaScript, PHP, Python, Ruby have an enumeration type.

### 6.4.3 Evaluation
Enums increase readability (named values are obvious) and reliability (in languages that forbid arithmetic on enums and assignment of out-of-range values — C#, F#, Java 5.0, Swift). C++ checks numeric assignments against the declared range, but if the user uses widely spread literals (e.g. `red=1, blue=1000, green=100000`), the check becomes useless. C provides neither benefit since it treats enum variables as integers.

## 6.5 Array Types
An **array** is a homogeneous aggregate; an element is identified by its **subscript** position relative to the first element.

### 6.5.1 Design Issues
- What types are legal for subscripts?
- Are subscript expressions range-checked?
- When are subscript ranges bound?
- When is array allocation done?
- Ragged or rectangular multidimensional arrays — or both?
- Can arrays be initialized at allocation?
- What kinds of slices are allowed?

### 6.5.2 Arrays and Indices
- Reference form: `array_name(subscript_list)` or `array_name[subscript_list]`. Arrays are "finite mappings" from subscripts to elements.
- Ada chose parentheses to mirror function calls (because both are mappings) — at a cost in readability (a reference like `B(I)` looks like a function call). Most other languages use brackets.
- Range checking is essential for reliability. Java, ML, and C# specify it; many earlier languages do not.

### 6.5.3 Subscript Bindings and Array Categories
Four categories, distinguished by when subscript ranges are bound and when storage is allocated:

| Category | Subscript ranges | Storage | Example |
|---|---|---|---|
| **Static** | Static | Static, before run time | C/C++ functions with `static` modifier |
| **Fixed stack-dynamic** | Static | Allocated on stack at declaration elaboration | C/C++ locals without `static` |
| **Fixed heap-dynamic** | Bound when allocated at run time | Heap, fixed after allocation | C `malloc`, Java non-generic arrays, C# arrays |
| **Heap-dynamic** | Dynamic, may change repeatedly | Heap, may shrink/grow | Perl arrays, C#'s `List<T>`, Java's `ArrayList`, Python lists, Ruby arrays, JavaScript arrays |

JavaScript arrays can be **sparse** — assigning `list[50] = 42` to a 10-element array gives length 51, with elements 11–49 undefined and not consuming storage. A reference to a missing element returns `undefined` (Python instead raises a run-time error; Ruby returns `nil`).

### 6.5.4 Array Initialization
C/C++/Java/Swift/C# allow initialization at allocation: `int list[] = {4,5,7,83};`. The compiler sets the length. For C/C++ char arrays, string literals like `"freddie"` create 8-element arrays (the null terminator is added). `char *names[]` produces an array of pointers, not an array of arrays.

### 6.5.5 Array Operations
- C-based languages provide essentially no whole-array operations.
- Perl supports array assignment but not array comparison.
- Python: assignment (reference change), catenation (`+`), membership (`in`), identity comparison (`is`), and structural equality (`==`).
- Ruby is similar: `==` compares structures elementwise.
- F# `Array` module: `Array.append`, `Array.copy`, `Array.length`.
- **APL** is the most powerful array-processing language ever devised — `A + B` works for scalars, vectors, or matrices; unary operators reverse, transpose, invert; the inner product operator `+.×` composes operators.

### 6.5.6 Rectangular and Jagged Arrays
- **Rectangular**: all rows have the same number of columns.
- **Jagged**: rows may have different lengths; possible when multi-D arrays are arrays of arrays. C/C++/Java support jagged but not rectangular. C# and F# support both: rectangular uses one bracket pair (`myArray[3, 7]`), jagged uses two (`myArray[3][7]`).

### 6.5.7 Slices
A **slice** is a substructure of an array referenced as a unit; it is not a new type but a reference mechanism. Python: `vector[3:6]` is elements 3–5. Stride form: `vector[0:7:2]` is every other element up to subscript 7. Perl: `@list[1..5]`. Ruby's `slice` method takes either an index, an `(index, count)` pair, or a range.

### 6.5.8 Evaluation
Two main advances since Fortran I: slices and dynamic arrays. The newest direction is associative arrays (next section).

### 6.5.9 Implementation of Array Types
Single-dimensional access function:
```
address(list[k]) = address(list[lower_bound]) + ((k - lower_bound) * element_size)
```
The constant part can be computed at compile time if the element type and base are static.

**Multidimensional arrays** are mapped to 1-D memory in **row major order** (almost universal) or column major order (rare). Row-major formula for an `n_rows × n_cols` matrix:
```
location(a[i,j]) = address(a[0,0]) + ((i * n_cols + j) * element_size)
```
The compile-time descriptor of a single-D array stores: element type, index type, lower/upper bound, address.

## 6.6 Associative Arrays
An **associative array** is an unordered collection of pairs in which each pair has a **key** and a **value**, and elements are referenced by their key.

- Perl: hashes; names begin with `%`, references via `$h{"key"}`. Operators: `delete`, `exists`, `keys`, `values`, `each`.
- Python: dictionaries (values are object references).
- Ruby: keys can be of any type (Perl keys must be strings; PHP keys can be int or string).
- PHP arrays are both normal and associative.
- Swift's are called dictionaries; keys must all be of one type.

### 6.6.2 Implementing Associative Arrays
Perl uses a 32-bit hash per entry; on expansion, only more bits of the hash are needed and only half of the entries move. PHP arrays additionally link elements in insertion order to support `current`/`next` iteration.

## 6.7 Record Types
A **record** is an aggregate of (possibly heterogeneous) elements, **fields**, identified by names and accessed via offsets from the beginning of the structure. Records ≠ heterogeneous arrays: record fields reside in adjacent memory of possibly different sizes; heterogeneous-array elements are references to scattered objects.

Records exist since COBOL (1960s). C/C++/C#/Swift use `struct`. C#'s `struct` is a **stack-allocated value type** (unlike classes, which are heap-allocated reference types). In Python and Ruby, records are commonly emulated via hashes.

### 6.7.1 Definitions of Records
COBOL uses **level numbers** to indicate hierarchical structure. `01 EMPLOYEE-RECORD. 02 EMPLOYEE-NAME. 05 FIRST PICTURE IS X(20). ...`

### 6.7.2 References to Record Fields
- **COBOL**: `field_name OF inner_record OF ... OF outer_record` (smallest first).
- **Dot notation**: `Employee_Record.Employee_Name.Middle` (largest first; opposite of COBOL).
- A **fully qualified reference** names all enclosing records.
- An **elliptical reference** omits some intermediate record names if unambiguous. (COBOL allows this; it complicates the compiler and hurts readability.)

### 6.7.3 Evaluation
Records and arrays are complementary: arrays for homogeneous, sequentially-processed data with dynamic subscripting; records for heterogeneous, field-by-field processing with **static** field references (which are also more efficient than dynamic subscripts and allow type checking).

### 6.7.4 Implementation of Record Types
Fields stored in adjacent memory; each field has a compile-time **offset** relative to the record start. The compile-time descriptor holds, per field, the name, type, and offset, plus the record's address. No run-time descriptor needed.

## 6.8 Tuple Types
A **tuple** is like a record except its elements have no names. Tuples appear in Python (immutable, written `(3, 5.8, 'apple')`, indexed with brackets), ML (must have ≥2 elements; element access with `#1(myTuple)`), F# (`(3, 5, 7)`, accessed via `fst`/`snd` for pairs, or by pattern-matching `let a, b, c = tup`), and Swift. Tuples are used in Python/ML/F# to return multiple values from functions.

## 6.9 List Types
Lists were first supported in **Lisp**.
- Scheme/Common Lisp: lists are parenthesized, no separators: `(A B C D)`. **Data and code share the same syntactic form** — `(A B C)` is also a call to function `A` with arguments `B` and `C`. Fundamental operators: `CAR` (first element), `CDR` (rest of list), `CONS` (prepend), `LIST` (build).
- ML: square brackets, comma-separated `[5, 7, 9]`; `[]` or `nil` is empty; `::` is CONS; functions `hd` (head) and `tl` (tail); elements must all be of the same type.
- F#: like ML but elements separated by semicolons; `hd`/`tl` are methods of `List`.
- Python lists are mutable, indexed from 0, with `del`. Python has **list comprehensions**: `[x*x for x in range(12) if x % 3 == 0]` returns `[0, 9, 36, 81]`. Originated in Haskell (`[n * n | n <- [1..10]]`). F# also has them.
- Java's `ArrayList` and C#'s `List<T>` are list classes.

## 6.10 Union Types
A **union** stores values of different types at different times.

### 6.10.1 Design Issues
Type checking — i.e. how to know which type is currently stored.

### 6.10.2 Discriminated vs Free Unions
- **Free union** (C and C++): no type checking — `union flexType` storing an int and then read as a float gives nonsense. Programmers have complete freedom (and therefore complete responsibility).
- **Discriminated union**: union carries a **tag** / **discriminant** identifying the current type. First in ALGOL 68; today in ML, Haskell, F#.

### 6.10.3 Unions in F#
```fsharp
type intReal =
  | IntValue of int
  | RealValue of float;;
```
`IntValue` and `RealValue` are **constructors**. Values are accessed via **pattern matching** with `match … with`, where the right-hand sides are guarded by patterns (possibly with `_` wildcards).

### 6.10.4 Evaluation
Unions are one reason C and C++ are not strongly typed — their unions are not type-checked. ML, Haskell, F# can use unions safely. Java and C# omit them entirely, reflecting safety concerns.

### 6.10.5 Implementation of Union Types
All variants share the same address; storage is reserved for the largest variant.

## 6.11 Pointer and Reference Types
A **pointer type** value is either a memory address or a special value `nil`. Pointers serve two purposes: (1) indirect addressing, and (2) management of **heap-dynamic** memory.

- Heap-dynamic variables are usually **anonymous** (no name); accessed only via pointers.
- Pointers are themselves **value types** (storing data — an address), as opposed to **reference types**.

### 6.11.1 Design Issues
- Scope and lifetime of the pointer variable.
- Lifetime of the heap-dynamic variable it points to.
- Restrictions on what the pointer can point to.
- Used for dynamic storage, indirect addressing, or both?
- Pointer types, reference types, or both?

### 6.11.2 Pointer Operations
Two fundamental operations: **assignment** (set a pointer's value) and **dereferencing** (access the value pointed at). Dereferencing may be explicit (`*ptr` in C/C++) or implicit. For pointers to records, C/C++ provide `p->age` as shorthand for `(*p).age`. Heap allocation is via `new` (C++/Java) or `malloc` (C). C++ deallocation: `delete`.

### 6.11.3 Pointer Problems
- **6.11.3.1 Dangling pointer / dangling reference** — a pointer to a heap-dynamic variable that has been deallocated. Explicit deallocation by the programmer is the cause; subsequent access is undefined and may corrupt the storage manager. Sequence: `p1` points to a new var, `p2 = p1`, then `p1`'s target is deallocated — `p2` is now dangling.
- **6.11.3.2 Lost heap-dynamic variable / memory leakage / garbage** — an allocated cell that is no longer accessible because the only pointer to it was reassigned. The cell can neither be used nor reclaimed.

### 6.11.4 Pointers in C and C++
C/C++ pointers can point at any variable, anywhere in memory. `&` is the address-of operator, `*` dereferences. **Pointer arithmetic** is scaled: `ptr + index` adds `index * sizeof(base type)`. For an array, `*(ptr+i)` ≡ `list[i]` ≡ `ptr[i]`. `void *` is a generic pointer (cannot be dereferenced without a cast). Functions can be pointed to; commonly used for parameter passing and callbacks.

### 6.11.5 Reference Types
A **reference type** refers to *an object or value*, not just an address — arithmetic on it makes no sense.
- C++ reference variables are **constant pointers that are always implicitly dereferenced**. Must be initialized when declared; cannot be re-bound. Primarily used for formal parameters (gives two-way communication without `*` clutter).
- Java removed C++-style pointers entirely. All Java references point to objects; references are not constants (can be re-bound). All class instances are accessed through references. Implicit deallocation → no dangling references.
- C# has both pointers (`unsafe`) and references; pointers exist mainly to interop with C/C++.
- Smalltalk, Python, Ruby: every variable is an implicitly-dereferenced reference.

### 6.11.6 Evaluation
Hoare (1973): pointers are "a step backward from which we may never recover." Yet they are necessary for low-level work (device drivers). Java/C# references give most of the flexibility without the hazards.

### 6.11.7 Implementation of Pointer and Reference Types
- **6.11.7.2 Solutions to dangling pointers.**
  - **Tombstones** (Lomet, 1975): every heap-dynamic variable is accessed via a per-variable indirection cell, the tombstone. On deallocation, the tombstone is set to `nil`. Costly in time (extra indirection) and space (tombstones are never reclaimed). Not used in any widely-used language.
  - **Locks and keys** (UW-Pascal): pointer values are pairs `(key, address)`; each heap cell stores a lock; every dereference checks the key against the lock. On deallocation, the lock is cleared.
  - **Implicit deallocation** (Lisp, Java, C#'s references) is the safest — programmers can't deallocate, so dangling pointers can't be created.
- **6.11.7.3 Heap Management.**
  - **Single-size cells.** Free cells linked in a free list. Deallocation needs to detect when a cell becomes unreachable.
    - **Reference counters / eager approach**: each cell stores a count of pointers to it; decremented when a pointer is disconnected; when 0, cell is reclaimed. Problems: (1) per-cell space overhead, (2) time overhead at every pointer change, (3) **circular structures** keep counts ≥ 1 and never get reclaimed. Improvement: deferred reference counting.
    - **Mark-sweep / lazy approach**: run only when free list is empty. Three phases — (1) set every cell's marker to "garbage", (2) **marking phase** traces every program pointer recursively (Schorr-Waite algorithm avoids stack overhead by reversing pointers), (3) **sweep phase** reclaims unmarked cells. Improvements: **incremental mark-sweep** runs more often; **partial collection** sweeps only part of memory at a time.
  - **Variable-size cells** add three additional problems: initial marker setting is harder (cells differ in size), tracing requires special structure (cells without natural pointer slots need internal pointers added), and free-list maintenance fragments space (eventually adjacent free blocks must be coalesced).

## 6.12 Optional Types
Variables of an **optional type** can either have a normal value or a special "no value" indicator. Supported in C#, F#, Swift, and others.
- C# value types declared with `?` (e.g. `int? x;`) can be `null`. Reference types are inherently optional. Test with `if(x == null)`.
- Swift uses `nil` instead of `null`. Syntax: `var Int? x;`.

## 6.13 Type Checking
- **Type checking** = ensuring operator operands have **compatible** types (operands and operators are generalized to include subprograms and assignment).
- A **compatible type** is either legal for the operator or can be implicitly converted (**coerced**) to one.
- A **type error** is application of an operator to an incompatible operand.
- **Static type checking** at compile time is preferred — finding errors earlier is cheaper.
- **Dynamic type checking** at run time is required when types are dynamically bound (JavaScript, PHP) or when union types are present.
- Even in statically-bound languages like C++, unions force some dynamic type checking.

## 6.14 Strong Typing
A language is **strongly typed** if **type errors are always detected** — statically or dynamically.
- **C and C++ are not strongly typed** (their unions are not type-checked).
- ML and F# are strongly typed.
- Java and C# are nearly strongly typed — casts can produce errors, but no implicit type errors slip past.
- **Coercion weakens strong typing.** Java's implicit `int → float` allows `a + d` (where `d` is a float) when the programmer meant `a + b` (both int). ML and F# have no coercions, so their error detection is stronger. C/C++ have heavy coercion; Java/C# have about half as many as C++.

## 6.15 Type Equivalence
Compatibility = legal for an operator (possibly via coercion). Equivalence = compatibility *without* coercion. Two main approaches:
- **Name type equivalence**: two variables have equivalent types iff they are declared in the same declaration or use the same type name. Easy to implement; restrictive. Requires all types to have names (anonymous types must be implicitly named by the compiler). Problem: an integer subrange wouldn't be equivalent to integer; structured types passed between subprograms must be defined globally.
- **Structure type equivalence**: equivalent iff identical structure. More flexible, harder to implement (recursive types, e.g. linked lists, complicate comparison). Cannot distinguish two semantically different types (e.g. `Celsius` and `Fahrenheit`) that happen to have the same structure.

**Ada** uses restrictive name equivalence plus two helpful constructs:
- **Derived type** (`type Celsius is new Float;`) — a new type with identical structure but not equivalent to the parent; inherits parent operations.
- **Subtype** (`subtype Small_type is Integer range 0..99;`) — a (possibly range-constrained) version of an existing type and **is type-equivalent to its parent**.
- Unconstrained arrays use **structure equivalence**: arrays of the same element type and length are equivalent regardless of subscript range. Constrained anonymous arrays each get a unique anonymous type — even `C, D : array(1..10) of Integer;` produces two non-equivalent types.

**C** uses both: name equivalence for `struct`, `enum`, `union` (each declaration creates a new type); structure equivalence for other non-scalars (arrays, etc.). `typedef` does **not** create a new type — it's just an alias. Exception: structs/enums/unions in different files use structure equivalence. **C++** is like C but without the cross-file exception.

## 6.16 Theory and Data Types
- A **type system** is a set of types plus rules governing their use.
- **Type map** (analogous to a state in denotational semantics) is a set of `(variable_name, type)` pairs, built from declarations. In static typing, only maintained at compile time (it is the symbol table); in dynamic typing, maintained at run time (often via tags on values).
- Structured types correspond to set-theoretic operations:
  - **Finite mapping** (function from domain to range) models arrays and functions.
  - **Cartesian product** `S1 × S2 × … × Sn` models tuples (and approximately records — records add field names).
  - **Set union** models union types.
  - **Subset** models Ada subtypes (loosely).
- Pointers are not defined via set operations.

---

<a id="chapter-7"></a>
# Chapter 7 — Expressions and Assignment Statements

Source for any section that defines the **evaluation rules** of expressions: precedence, associativity, operand evaluation, conversions, and side-effects. Builds on Ch 3 syntax and Ch 5 (variables).

## 7.1 Introduction
Expressions are the fundamental means of specifying computations. To understand expression evaluation we must understand the **order of operator evaluation** (precedence + associativity) and the **order of operand evaluation** (affected by side effects). Assignment statements are the central construct in imperative languages — their purpose is to cause the side effect of changing variable values (the program state).

## 7.2 Arithmetic Expressions
Operators are **unary** (1 operand), **binary** (2), or **ternary** (3). Most are **infix**; some are **prefix** (Lisp/Scheme have all prefix; Perl has some); `++` and `--` in C-based languages can be prefix or postfix.

### Design issues
- Operator precedence rules.
- Operator associativity rules.
- Order of operand evaluation.
- Restrictions on operand evaluation side effects.
- User-defined operator overloading?
- What type mixing is allowed in expressions?

### 7.2.1 Operator Evaluation Order
**7.2.1.1 Precedence.** Operators are arranged in a hierarchy. In most imperative languages: exponentiation (highest, when provided), then `*` `/` `%`, then `+` `-` (binary), with unary `+`/`-`/`++`/`--` typically just above multiplication. Unary `+` is the **identity operator** (Ellis & Stroustrup call it useless). Exponentiation operator (`**` in Fortran, Ruby, Visual Basic, Ada; `^` in Visual Basic). Where provided, `**` has **higher precedence than unary minus** so `-A**B` ≡ `-(A**B)`.

In **APL** all operators have the **same precedence** — order is determined entirely by associativity (right to left).

**7.2.1.2 Associativity.** When two adjacent operators have the same precedence, associativity decides which comes first.
- **Left associative**: most binary operators in most languages. `a - b + c` → `(a - b) + c`.
- **Right associative**: exponentiation in Fortran/Ruby (`A**B**C` → `A**(B**C)`); unary `-`, `+`, `++`, `--` in C-based languages. In Visual Basic, `^` is left-associative.
- **APL** is right-to-left for everything: `A × B + C` with A=3, B=4, C=5 evaluates to `3 × (4+5) = 27`.

Floating-point addition is **mathematically associative but not associatively safe on a computer** because of overflow and precision: `A+B+C+D` may overflow if reordered into a different sum even when the mathematically-equivalent groupings would not. So compiler reordering of float operations can change results.

**7.2.1.3 Parentheses.** Force the desired order. APL dispensed with precedence rules entirely — the programmer parenthesizes everything. Tedious but simple.

**7.2.1.4 Ruby Expressions.** All arithmetic, relational, assignment, indexing, shift, and bitwise operators are **methods** on objects; `a + b` is `a.+(b)`. This is why operators can be overridden by user code in Ruby. In C++ and Ada, operators are implemented as **function calls**.

**7.2.1.5 Expressions in Lisp.** All arithmetic and logic operations are subprograms, *explicitly* called. `a + b * c` in Lisp is `(+ a (* b c))`.

**7.2.1.6 Conditional Expressions.** `expr1 ? expr2 : expr3` (C-based languages, Perl, JavaScript, Ruby). The `?` is a **ternary operator**. The whole construct can appear anywhere any expression can. Both branches mandatory.

### 7.2.2 Operand Evaluation Order
If neither operand of an operator has side effects, the order is irrelevant. The only interesting case is when an operand has a side effect.

**7.2.2.1 Side Effects.** A **functional side effect** occurs when a function changes one of its parameters or a global variable. Example:
```c
int a = 5;
int fun1() { a = 17; return 3; }
void main() { a = a + fun1(); }
```
If `a` is fetched before `fun1()` is called, the result is `8`; if `fun1()` is evaluated first, `a` becomes `17`, then `17 + 3 = 20`. Two possible language-design solutions: (1) disallow functional side effects entirely (hard in C-style languages because all subprograms are functions), or (2) require a specific evaluation order. **Java guarantees left-to-right operand evaluation**, eliminating this ambiguity.

**7.2.2.2 Referential Transparency and Side Effects.** A program has **referential transparency** if two expressions with the same value can be substituted for each other anywhere without changing the program. A referentially-transparent function depends only on its parameters; it cannot have side effects. Programs in pure functional languages are referentially transparent because they have no mutable variables.

## 7.3 Overloaded Operators
**Operator overloading** = multiple uses of an operator depending on operand types. `+` for integer add, float add, and string catenation (in Java) is acceptable if it doesn't hurt readability/reliability.

Dangers of overloading:
- C++'s `&` is bitwise AND (binary) but **address-of** (unary). Two completely unrelated meanings on the same symbol. A missing first operand in a bitwise AND is silently interpreted as an address-of.
- Unary vs binary minus has the same issue, though semantically related.

C++, C#, F# allow user-defined operator overloading. C++ disallows overloading of `.` and `::`. Java omitted operator overloading (it's back in C#).

When sensibly used, user-defined overloading helps readability (matrix `A*B + C*D` vs `MatrixAdd(MatrixMult(A,B), MatrixMult(C,D))`). Harmful when redefining `+` to mean multiplication, etc.

## 7.4 Type Conversions
- **Widening conversion**: target type can include (at least approximations of) all source values. Usually safe but may lose precision (e.g. 32-bit int to 32-bit float loses 2 digits).
- **Narrowing conversion**: target cannot store all source values; sometimes unsafe (large float to int can produce a meaningless result).
- **Explicit** conversions (programmer-requested) are called **casts**.
- **Implicit** conversions (compiler-inserted) are called **coercions**.

### 7.4.1 Coercion in Expressions
**Mixed-mode expressions** (operands of different types) require coercion rules. Trade-off:
- Languages with many coercions (C, C++) are flexible but unreliable: keying error `d = b * a;` (intending `b * c`, both `c` and `d` are float, `a` is int) goes undetected because `a` is silently coerced.
- **F#, Ada, and ML disallow mixed-mode expressions** (no int+float).
- In Java, `byte` and `short` are coerced to `int` for almost any operator.

### 7.4.2 Explicit Type Conversion
- C-style cast: `(int)angle`. Parentheses around the type were chosen partly because C has two-word type names like `long int`.
- ML and F# use function-call syntax: `float(sum)`.

### 7.4.3 Errors in Expressions
Possible run-time errors during expression evaluation:
- **Overflow / underflow** (result too large / small for the target cell).
- **Division by zero**.

These are run-time errors called **exceptions** (Chapter 14).

## 7.5 Relational and Boolean Expressions

### 7.5.1 Relational Expressions
A **relational operator** compares two operands; the value is Boolean (or numeric when no Boolean type, as in C89). Operand types are usually numerics, strings, enums.

Inequality syntax: C-based `!=`, Fortran 95+ `.NE.` or `<>`, ML/F# `<>`.

JavaScript/PHP add `===` and `!==` — like `==`/`!=` but **without coercion**. `"7" == 7` is true (string coerced to number); `"7" === 7` is false. Ruby uses `==` (with coercion) and `eql?` (no coercion). Ruby's `===` is only used in `case` statements.

Relational operators have lower precedence than arithmetic.

### 7.5.2 Boolean Expressions
Boolean expressions use Boolean variables, constants, relational expressions, and Boolean operators (AND, OR, NOT, sometimes XOR and equivalence).

In Boolean algebra, AND and OR have **equal precedence**. In C-based languages, AND is **higher** precedence than OR (a baseless correlation with `×` and `+`).

C-based precedence (highest → lowest):
```
postfix ++  --
unary +, -, prefix ++, --, !
*, /, %
binary +, binary -
<, >, <=, >=
==, !=
&&
||
```

C89 has no Boolean type — numerics are used (0 = false, nonzero = true). Result of evaluation is 0 or 1. One odd consequence: `a > b > c` is legal in C: `a > b` produces 0 or 1, which is then compared with `c` (so `b` is **not** compared to `c`).

Perl/Ruby provide two sets: `&&`/`||` and `and`/`or`. The spelled forms have lower precedence; `and`/`or` have equal precedence, but `&&` is higher than `||`.

Readability dictates that a Boolean type should exist; numeric-as-Boolean costs error detection.

## 7.6 Short-Circuit Evaluation
A **short-circuit evaluation** computes the result without evaluating all operands.
- Arithmetic: `(13 * a) * (b / 13 - 1)` is 0 when `a == 0`, but compilers don't take this shortcut at run time.
- Boolean: `(a >= 0) && (b < 10)` — if `a < 0`, the right side need not be evaluated; this **is** done by language implementations.

Why it matters: in `while ((index < listlen) && (list[index] != key)) ...`, without short-circuit evaluation the `list[index]` reference would be out-of-bounds when `index == listlen`, causing a run-time error.

Caveat: a side-effecting operand inside a short-circuited Boolean can fail to fire if the short-circuit is taken (e.g. `(a > b) || ((b++) / 3)` — `b++` runs only if `a <= b`).

- C-based: `&&` and `||` are short-circuit; `&` and `|` are bitwise (not short-circuit).
- Ruby, Perl, ML, F#, Python: all logical operators are short-circuit.

## 7.7 Assignment Statements

### 7.7.1 Simple Assignments
Most languages use `=` for assignment, requiring a different symbol for equality. ALGOL 60 (and Ada) use `:=` to avoid confusion. In Fortran and Ada, an assignment can appear only as a standalone statement; in many languages it has many other uses (next subsections).

### 7.7.2 Conditional Targets
Perl: `($flag ? $count1 : $count2) = 0;` is equivalent to an if/else that assigns to one or the other.

### 7.7.3 Compound Assignment Operators
Introduced by ALGOL 68; adopted by C-family, Perl, JavaScript, Python, Ruby. `sum += value` ≡ `sum = sum + value`.

### 7.7.4 Unary Assignment Operators
C-based, Perl, JavaScript: `++` and `--`. Prefix `++count` increments then uses the value; postfix `count++` uses the value then increments. Right-to-left association for adjacent unaries: `-count++` means `-(count++)`.

### 7.7.5 Assignment as an Expression
In C-based, Perl, JavaScript: an assignment is an expression that produces the assigned value. Idiom: `while ((ch = getchar()) != EOF) { ... }`. **Parentheses are required** because `=` has lower precedence than `!=`. Disadvantages:
- Side effects in expressions hurt readability.
- Allows multi-target chains: `sum = count = 0` (also legal in Python).
- Allows the classic bug: `if (x = y)` silently assigns instead of comparing. Java and C# disallow this by requiring `boolean` in `if` controls.

### 7.7.6 Multiple Assignments
Perl: `($first, $second, $third) = (20, 40, 60);`. Swap without a temp: `($first, $second) = ($second, $first);`. Ruby supports similar syntax without parentheses.

### 7.7.7 Assignment in Functional Languages
Identifiers in pure functional languages are **names of values**, never reassigned. ML's `val cost = quantity * price;` binds a name; a later `val cost = ...` creates a new version that hides the previous. F#'s `let` is similar; `let` creates a new scope, `val` does not.

## 7.8 Mixed-Mode Assignment
- C/C++/Perl: same loose coercion rules as for expressions.
- **Java and C# allow mixed-mode assignment only if the required coercion is widening** — `int` can go into `float`, not the other way. Halves the possible mismatched assignments compared to C++.
- Functional languages: no such thing as a mixed-mode assignment (assignments only name values).

---

<a id="chapter-8"></a>
# Chapter 8 — Statement-Level Control Structures

This chapter is the source for the **control-flow** section of any DSL design document: selection, iteration, and unconditional branches. **Böhm and Jacopini** proved that **sequence + two-way selection + pretest logical loops** are sufficient to express any computation, but languages add more for writability and readability.

## 8.1 Introduction
- A **control structure** is a control statement together with the statements whose execution it controls.
- One design issue applies to every control structure: should multiple **entries** be allowed? Modern consensus: no — multiple entries hurt readability more than they help flexibility, and they only exist via `goto`/labels anyway.
- Multiple **exits** are universally allowed because restricting exits to fall-through is harmless. Goto-based exits are dangerous; restricted exits (`break`, `last`) are fine.

## 8.2 Selection Statements
Two categories: **two-way** and **n-way (multiple) selection**.

### 8.2.1 Two-Way Selection Statements
Form: `if <control_expression> then-clause [else-clause]`.

**8.2.1.1 Design issues.**
- Form and type of the control expression.
- How the then/else clauses are specified.
- How nested selectors are disambiguated.

**8.2.1.2 The Control Expression.** Parenthesized in C-based languages (no `then` keyword). In Ruby, the `then` keyword removes the need for parens. C89/C99/C++/Python allow arithmetic expressions as controls; most modern languages require Boolean.

**8.2.1.3 Clause Form.** Many languages: single or compound statement. Perl: must always be compound. Python and Ruby: statement *sequences* terminated by a reserved word. Python uses indentation to delimit; Ruby uses `end`.

**8.2.1.4 Nesting Selectors — the Dangling-Else Problem.** Recall Ch 3: the grammar `<if_stmt> → if <e> then <s> | if <e> then <s> else <s>` is ambiguous when an if is nested inside another's then-clause with no intervening else. Resolutions:
- **Static-semantics rule** (Java/C/C++/C#): `else` always matches the nearest unpaired `then`. Doesn't help when the programmer wanted the opposite — must use braces.
- **Mandatory compound clauses** (Perl): syntax forces unambiguous grouping.
- **Reserved word terminator** (Ruby `end`, Python relies on indentation): the closing token makes the structure explicit.

ML avoids the problem by **disallowing else-less ifs**.

**8.2.1.5 Selector Expressions.** In functional languages (ML, F#, Lisp), `if` is an **expression** — it returns a value. F# requires then and else clauses to return the same type; an else-less `if` may return only **unit** (a special "no value" type, written `()`).

### 8.2.2 Multiple-Selection Statements

**8.2.2.1 Design issues.**
- Form and type of the selector expression.
- How segments are specified.
- Is execution restricted to a single segment per execution?
- Form of case values.
- What happens if no case matches?

**8.2.2.2 Examples.**
- **C / C++ / Java / JavaScript `switch`** is a primitive design: control + constant expressions must be discrete (integer / char / enum); selectable items can be statement sequences, compound statements, or blocks; **no implicit branches between segments** — fall-through is the default. **`break` is used to exit a segment**. The default segment is optional; if no case matches and no default, the switch does nothing. The trade was reliability for flexibility (in case the programmer wants fall-through); studies show fall-through is rarely used and missing `break`s cause subtle bugs.
- **C# `switch`** fixes this: every selectable segment must end with an unconditional branch (`break` or `goto case`), so implicit fall-through is forbidden. Strings are also allowed as the control and case values.
- PHP `switch`: like C's but case values can be string/int/double.
- **Ruby `case`** has two forms; one is semantically a list of nested `if`s:
  ```ruby
  leap = case
         when year % 400 == 0 then true
         when year % 100 == 0 then false
         else year % 4 == 0
         end
  ```
  Perl and Python have no multiple-selection statement.

**8.2.2.3 Implementing Multiple Selection.** Three approaches by case count:
- **<10 cases**: a chain of conditional branches or a linear table search.
- **≥10 cases**: build a **hash table** of segment labels — equal-time lookup. Doesn't work if ranges are allowed.
- **Range-based cases (Ruby)**: build a **binary search table** of (value, address) pairs.
- **Small dense range**: build an **array** indexed by case value with default-segment fillers for unused indices — pure array indexing, very fast.

**8.2.2.4 Multiple Selection Using if.** When choice is based on Boolean expressions (not on the value of a single ordinal expression), `switch` is inadequate. Languages like Python (`elif`) and Perl provide a syntactic shorthand for else-if chains. The else-if statement is **not** a redundant form of switch — switches restrict each branch to comparing one expression to a constant, while else-if chains permit arbitrary Boolean tests. Scheme's `COND` is a similar mathematical-conditional-expression construct: `(COND (predicate1 expr1) ... [(ELSE exprN)])`.

## 8.3 Iterative Statements
An **iterative statement** (loop) executes a body 0+ times.

### Design questions
- How is iteration controlled? **Logical, counting, or both?**
- Where is the control mechanism? **Top** (pretest) or **bottom** (posttest)?
- The **body** is the controlled collection of statements.

### 8.3.1 Counter-Controlled Loops
A **loop variable** holds the count; **loop parameters** are initial value, terminal value, and **stepsize**.

**8.3.1.1 Design issues.**
- Type and scope of the loop variable.
- Can the loop variable or loop parameters be changed in the loop?
- Are loop parameters evaluated once or every iteration?
- What is the loop variable's value after termination? (Fortran 90: undefined. Ada: loop variable's scope is the loop itself, so the question doesn't arise.)

**8.3.1.2 The C-based `for`.** General form: `for (expr1; expr2; expr3) loop_body`. Three expressions are optional; absent `expr2` is treated as true (potentially infinite). expr1 runs once; expr2 is tested each iteration; expr3 runs after each iteration. Most flexible — there is no required loop variable; loop variables can be of any type, multiple loop variables can be combined with commas, body can be empty. C99/C++/Java/C# allow variable definitions in expr1 (scope = the loop); Java/C# require boolean for expr2.

**8.3.1.3 Python's `for`.** Iterates over an iterable: `for var in object: body [else: else_clause]`. The loop variable can be changed in the body but it doesn't affect control. The `else` clause runs if the loop terminates normally (not via `break`). Combined with `range(n)`, `range(start, stop)`, or `range(start, stop, step)` for counting loops. `range` never returns the upper bound.

**8.3.1.4 Counter-Controlled Loops in Functional Languages.** Pure functional languages use **recursion**, not iteration. A counting loop is built with a recursive function whose parameters include the body (a function value) and the remaining repetition count.

### 8.3.2 Logically-Controlled Loops
More general than counter loops: every counter loop can be built with a logical loop but not vice versa.

**8.3.2.1 Design issues.**
- Pretest or posttest?
- Special form of the counting loop, or a separate statement?

**8.3.2.2 Examples.** C-based languages have both as **separate statements**:
- **Pretest** `while (control) body`
- **Posttest** `do body while (control);`

Java/C# require boolean controls. Posttest loops are infrequently useful and somewhat dangerous because the body always runs at least once.

### 8.3.3 User-Located Loop Control Mechanisms
Loops with infinite-loop structure plus user-located exits.

**Design issues.**
- Is the exit conditional or unconditional?
- Can it exit only one loop or multiple enclosing loops?

- **C, C++, Python, Ruby, C#**: unconditional unlabeled `break`.
- **Java, Perl**: unconditional labeled exits (`break` with a label in Java, `last` with a label in Perl). Java example:
  ```java
  outerLoop:
  for (row = 0; row < numRows; row++)
    for (col = 0; col < numCols; col++) {
      sum += mat[row][col];
      if (sum > 1000.0) break outerLoop;
    }
  ```
- **`continue`** (C, C++, Python) skips the rest of the current iteration but does **not** exit the loop.

User-located exits fulfill the legitimate need for `goto` (premature exit) without `goto`'s harm.

### 8.3.4 Iteration Based on Data Structures
A data-based iteration statement uses an **iterator** function (history-sensitive) that returns successive elements until exhausted.

- C's `for` is flexible enough to simulate this: `for (ptr = root; ptr != null; ptr = traverse(ptr))`.
- **Java 5.0's enhanced `for`** ("foreach", still spelled `for`) iterates over arrays and `Iterable` collections: `for (String elem : myList) { ... }`.
- **C#/F#'s `foreach`** works on all .NET collections; users can implement `IEnumerator` for custom collections.
- **Ruby blocks**: a method call followed by a `{ ... }` or `do ... end` block. The block is an anonymous method passed as a parameter. Built-in iterators include `times`, `upto`, and `each`. Block parameters appear between vertical bars: `list.each {|value| puts value}`. The `yield` keyword inside a method calls the block. Ruby has no `for` statement — `for x in 1..5` is rewritten to `1.upto(5)`.
- **Python generators**: any function containing `yield` is a generator. Subsequent calls to the generator resume execution after the previous `yield`, preserving local state. Used as iterators.

## 8.4 Unconditional Branching
The **`goto`** statement transfers control to a label. The most powerful control statement (all others can be built from goto + selection), but the easiest to abuse.

Edsger Dijkstra's 1968 letter ("Goto Statement Considered Harmful") triggered the debate. Knuth (1974) argued for occasional efficiency-based use. Java, Python, Ruby have removed `goto`. C, C++, C# keep it. C#'s `goto` is restricted but legal — used in `switch` segments. **All user-located loop exits are restricted gotos** — they help readability rather than hurt it because their targets are constrained.

## 8.5 Guarded Commands
Dijkstra (1975) — designed to support correctness reasoning *during* development (not just verification after).

### Selection
```
if  <Boolean expression> -> <statement>
[]  <Boolean expression> -> <statement>
[]  …
fi
```
- Each line is a **guarded command**: a Boolean **guard** and a statement.
- All guards are evaluated; if more than one is true, **one is chosen non-deterministically**.
- If **no** guard is true, this causes a **run-time error**. This forces the programmer to enumerate all cases.
- Example: pick the larger of x and y. With `[]` between two `>=` guards, when x = y, either branch can be chosen — and the program is correct either way. This is abstraction-by-nondeterminism.

### Iteration
```
do  <Boolean expression> -> <statement>
[]  <Boolean expression> -> <statement>
od
```
- All guards evaluated each pass; one true branch is chosen (non-deterministically) and executed; loop repeats.
- Terminates when **all guards are simultaneously false**.

Significance for the project: guarded commands show how syntax and semantics affect formal program verification. Verification is virtually impossible with `goto`s, much simpler with logical loops + selection or with guarded commands. Guarded commands have a clean axiomatic semantics (Gries, 1981).

## 8.6 Conclusions
- Only sequence + selection + pretest logical loops are theoretically required (Böhm & Jacopini), but languages add more for writability/readability.
- The right design choice balances number of control statements against simplicity and readability. Wirth and Hoare both favor minimal designs.
- Modern languages still disagree on whether to include `goto` (yes: C++, C#; no: Java, Ruby).

---

<a id="chapter-9"></a>
# Chapter 9 — Subprograms

This chapter is the source for the section of the deliverable that describes **how the DSL's functions/procedures behave**, including parameter passing, scoping inside subprograms, return semantics, and any generic/overloaded/anonymous-function features.

## 9.1 Introduction
Two abstraction facilities for languages: **process abstraction** (subprograms — central since the earliest high-level languages) and **data abstraction** (since the 1980s — Chapter 11). Subprograms make programs shorter, more readable, and abstract details away from the call site.

## 9.2 Fundamentals of Subprograms

### 9.2.1 General Characteristics
All non-coroutine subprograms:
1. **Single entry point.**
2. **The calling unit is suspended during execution** — only one subprogram active at any moment.
3. **Control returns to the caller on termination.**

Coroutines (§9.13) and concurrent units (Ch 13) violate (1) and (2).

### 9.2.2 Basic Definitions
- A **subprogram definition** describes interface + actions.
- A **subprogram call** is the request that one be executed.
- A subprogram is **active** if it has begun execution but not yet completed it.
- A **subprogram header** specifies (a) what kind of subprogram it is, (b) its name (if any), (c) its parameters.
- The **body** defines the actions. Delimited by braces (C-family, JavaScript), by `end` (Ruby), or by indentation (Python).
- Python `def` statements are **executable** — if `def` is inside an `if`, only the chosen version is available.
- The **parameter profile** = number, order, types of formal parameters.
- The **protocol** = parameter profile **+ return type** (for functions).
- A **subprogram declaration** gives the protocol but not the body. In C/C++ these are **prototypes**, often in header files. Needed for static type checking when forward references are allowed.

### 9.2.3 Parameters
Two ways a non-method subprogram accesses data: **direct access to nonlocals** (less reliable — extra exposure) or **parameter passing** (more flexible — a parameterized computation).

- **Formal parameters** appear in the header (names local to the subprogram); they are "dummy variables" — bound to storage only on a call.
- **Actual parameters** appear in the call.
- **Positional parameters**: bound by position; first actual → first formal, etc.
- **Keyword parameters**: actual parameters name their corresponding formal: `sumer(length=my_length, list=my_array, sum=my_sum)`. Allow any order. Disadvantage: caller must know formal names.
- **Default values**: Python, Ruby, C++, PHP support defaults. In C++ (no keywords), default parameters must appear last and once one is omitted, all later must be omitted too.
- **Variable number of parameters**: C/C++/Perl/JavaScript permit fewer actuals than formals (dangerous, but useful for `printf`). C#'s `params` allows variable-length arguments of one type, packaged as an array. Ruby's **array formal parameter** (prefixed `*`) absorbs all remaining actuals into an array.

### 9.2.4 Procedures and Functions
- **Functions** return a value; called inside expressions; semantically modeled on mathematical functions. Side-effect-free functions are ideal; most languages allow them anyway.
- **Procedures** do not return values; called as standalone statements; effectively define new statements.
- Most modern languages have only functions and let users write "procedure-style" functions returning void. Fortran and Ada are among the few with both.
- Functions can be viewed as defining **new operators** (e.g. C++'s `pow(x,y)`).

## 9.3 Design Issues for Subprograms
- Are local variables static or dynamic?
- Can subprogram definitions be nested?
- Parameter-passing method(s)?
- Are types of actual parameters checked against formal types?
- If subprograms can be passed as parameters and can be nested, what is the referencing environment of a passed subprogram?
- Are functional side effects allowed?
- What types and how many values can be returned?
- Can subprograms be **overloaded**? **generic**?
- If subprograms can be nested, are **closures** supported?

## 9.4 Local Referencing Environments

### 9.4.1 Local Variables
- **Stack-dynamic** locals are bound to storage on each call and freed on return.
  - ✅ Required for recursion.
  - ✅ Storage can be shared with locals of other (currently inactive) subprograms.
  - ❌ Allocation/initialization/deallocation cost per call.
  - ❌ Access is **indirect** (location only known at run time).
  - ❌ Subprograms **cannot be history-sensitive** (no carry-over between calls).
- **Static** locals are bound to storage for the entire program's lifetime.
  - ✅ Slightly faster (no allocation overhead, direct addressing).
  - ✅ Allow history sensitivity (e.g. pseudorandom generators).
  - ❌ **Cannot support recursion** (no per-activation copies).
  - ❌ Storage cannot be shared with other inactive subprograms.

In C/C++, locals are stack-dynamic by default; the `static` keyword forces static lifetime for a local. Java/C#/C++ methods have only stack-dynamic locals. Python locals are stack-dynamic; global variables must be explicitly declared `global` inside a method if assigned to (otherwise an assignment makes the name local).

### 9.4.2 Nested Subprograms
Originated in ALGOL 60. Hide helper subprograms inside their callers and give them privileged access to enclosing scopes (via static scoping). Long supported only by ALGOL descendants (ALGOL 68, Pascal, Ada). C-family did **not** allow nesting. **JavaScript, Python, Ruby** allow it again, as do most functional languages.

## 9.5 Parameter-Passing Methods

### 9.5.1 Semantics Models
Formal parameters have one of three **modes**:
- **In mode** — receive data from the caller only.
- **Out mode** — send data back to the caller only.
- **Inout mode** — both directions.

### 9.5.2 Implementation Models
Five implementation strategies have been used:

#### 9.5.2.1 Pass-by-Value (in mode)
The value of the actual parameter initializes the formal, which then acts as a local variable.
- ✅ Fast for scalars (linkage time + access time).
- ❌ Requires extra storage in the called subprogram for the formal.
- ❌ For large parameters (e.g. arrays) the copy is costly.
- Implementation: normally by copy. By access path is possible but requires write-protection of the cell.

#### 9.5.2.2 Pass-by-Result (out mode)
Nothing is transmitted in; the formal acts as a local variable; on return its value is copied to the actual parameter (which must therefore be a variable, not a literal/expression).
- ❌ Same storage/copy costs as pass-by-value.
- ❌ **Actual parameter collision**: `sub(p1, p1)` — the result depends on which formal is copied back last.
- ❌ Implementor may evaluate target addresses on call **or** on return; different choices produce different results (e.g. `f.DoIt(list[sub], sub)` where `sub` changes inside `DoIt`).

#### 9.5.2.3 Pass-by-Value-Result / Pass-by-Copy (inout mode)
Combines pass-by-value and pass-by-result. Formal initialized from actual; updated value copied back on return.
- ❌ Shares both sets of disadvantages: multiple storage, copying, plus collision and timing problems.

#### 9.5.2.4 Pass-by-Reference (inout mode)
Transmits an **access path** (usually an address); the called subprogram shares storage with the caller's actual parameter.
- ✅ Efficient — no copies.
- ❌ Access is slower (one extra indirection).
- ❌ **One-way intent is not enforced** — accidental writes are possible.
- ❌ **Aliasing** is easy to create:
  1. **Same actual passed twice**: `fun(total, total)` → `first` and `second` aliased.
  2. **Same array element via two indices**: `fun(list[i], list[j])` when `i == j`.
  3. **Element and whole-array**: `fun1(list[i], list)` — `fun1` can change `list[i]` via either parameter.
  4. **Nonlocal aliasing**: a global pointer also passed as a parameter.

#### 9.5.2.5 Pass-by-Name (inout mode)
The actual parameter is **textually substituted** for each occurrence of the formal in the body. Binding to a value or address is delayed until the formal is referenced/assigned. Requires passing a closure (the access expression plus its referencing environment); these closures were called **thunks** in ALGOL 60. Complex, inefficient, only in ALGOL 60 and SIMULA 67 among popular languages. Modern relatives: macros and the generic-parameter mechanism in C++/Java 5.0/C# 2005.

### 9.5.3 Implementing Parameter-Passing Methods
Parameter communication usually goes through the **run-time stack** (Ch 10).
- Pass-by-value: value copied into stack slot.
- Pass-by-result: stack slot used for return-out at termination.
- Pass-by-value-result: stack slot initialized from actual, used as local, then copied back.
- Pass-by-reference: only the **address** is placed in the stack; access is via indirection.

Fortran is unusual: it passes *all* parameters by reference, even literals and expressions (which are evaluated first into a temporary and that temporary's address is passed).

### 9.5.4 Methods of Some Common Languages
- **C**: pass-by-value. Pass-by-reference simulated by passing pointers (explicit `*` dereference in the called function).
- **C++**: pass-by-value or pass-by-reference via reference types (`int &p`). **Constant reference parameters** (`const int &p`) provide pass-by-reference efficiency with in-mode semantics — implicit write protection.
- **Java**: all parameters by value, but since object variables are references, objects are effectively passed by reference. Scalars cannot be modified by the callee (no scalar pass-by-reference).
- **C#**: pass-by-value by default. `ref` on both formal and actual forces pass-by-reference; `out` is similar but without requiring initial value (out-mode).
- **PHP**: like C#, but pass-by-reference can be specified on either formal or actual with `&`.
- **Swift**: default is by value; `inout` keyword on the formal makes it pass-by-reference.
- **Perl**: actuals implicitly placed in the predefined array `@_`; elements are aliases of the actuals (so modifying `$_[i]` changes the corresponding actual).
- **Python and Ruby**: **pass-by-assignment / pass-by-sharing**. Variables are references to objects; passing assigns the reference to the formal. Pass-by-reference semantics apply only when the object is mutable and mutated in place; reassigning the formal has no effect on the caller. So `x = x + 1` in the callee binds the local name to a new object — no effect on the caller. But mutating an array element (`list[3] = 47`) is visible.

### 9.5.5 Type Checking Parameters
Modern software engineering requires type checking parameters. Original Fortran/C did not check.

C has two declaration styles:
- **Old C (K&R)** style: parameter names in parens, types declared after. **No type checking.**
- **Prototype style** (C89+): types in the parameter list — types are checked; legal coercions (widening) are applied.

C99/C++ require prototypes for all functions. The `...` (ellipsis) skips checking for trailing parameters (used by `printf`).

C# requires `ref` parameter types to **exactly match** their actual parameter types — no coercion (because a coerced value might overflow the actual's location on return).

Python and Ruby do no parameter type checking — variables are typeless (only objects have types).

### 9.5.6 Multidimensional Arrays as Parameters
The compiler must build the array's storage-mapping function from the formal alone. In **C/C++** (row-major) the mapping needs `n_cols`, so the formal must specify all dimensions except the first: `void fun(int matrix[][10])`. This prevents writing a function that accepts matrices of differing column counts; the workaround is to pass a raw pointer plus the dimensions, and compute element addresses manually (or via a macro).

In **Java and C#**, arrays are objects with a `length`/`Length` member, so the formal needs no dimension info: `float sumer(float mat[][])` works for any sized matrix; nested arrays may even have different lengths per row.

### 9.5.7 Design Considerations
Two main concerns: **efficiency** and **direction of data flow**. Software engineering prefers minimal access (in mode for input-only, etc.), but a large in-only array is often passed by reference for efficiency — C++'s `const` reference parameters reconcile this conflict.

### 9.5.8 Examples of Parameter Passing
- **Pass-by-value (C `swap1`)**: in `void swap1(int a, int b) { ... }` with `swap1(c,d);`, the values inside are swapped but `c` and `d` outside are unchanged.
- **Pass-by-reference via pointers (C `swap2`)**: `swap2(&c, &d)` — works.
- **Pass-by-reference (C++ `swap2`)** with `int &a, int &b` — also works.
- **Pass-by-value-result (Ada `swap3`)**: works for two distinct variables. Also works correctly for `swap3(i, list[i])` **if addresses are computed at the call site** (so that `list[i]`'s address is fixed before the swap changes `i`).
- **Aliasing example with `fun(i, list[i])` and global `i`**: pass-by-value-result and pass-by-reference give different results because pass-by-reference shares the global and reflects changes during the body, while pass-by-value-result captures the old value and writes back unchanged.

## 9.6 Parameters That Are Subprograms
Often needed (e.g. numerical integrator parameterized by the function to integrate). Two complications:

1. **Type checking the passed subprogram's parameters.** In C/C++ functions can't be passed, but pointers to functions can; the pointer's type includes the protocol, so calls through it can be fully type-checked.
2. **Referencing environment of the passed subprogram** (only an issue when nested subprograms exist):
   - **Shallow binding** — environment of the **call** that activates the passed subprogram. Suited to dynamic-scoped languages.
   - **Deep binding** — environment of the **definition** of the passed subprogram. Natural for static-scoped languages.
   - **Ad hoc binding** — environment of the call that **passed** the subprogram. Never used in practice — no natural relationship.

JavaScript example (where x is locally defined in `sub1`, `sub3`, and `sub4`, and `sub2` references `x`): shallow → x from `sub4` (4); deep → x from `sub1` (1); ad hoc → x from `sub3` (3).

When the subprogram that declares another also passes it, deep and ad hoc are the same.

## 9.7 Calling Subprograms Indirectly
Needed when the specific subprogram is unknown until run time. Two main uses: **event handling** in GUIs and **callbacks**.

- **C/C++** support **pointers to functions** typed by protocol: `float (*pfun)(float, int);`. A bare function name is its address. Indirect call: `pfun2(first, second)` (the `*` is optional). Function pointers can be passed and returned.
- **C# delegates** are *objects* that wrap a method reference with a specified protocol:
  ```csharp
  public delegate int Change(int x);
  Change chgfun1 = fun1;        // assign a method to a delegate
  chgfun1(12);                  // call through the delegate
  ```
  Delegates support **multicast** via `+=`: a delegate may hold a list of methods, all of which are called in insertion order. Only the value returned by the last is kept. Used for .NET event handling and to implement closures.
- Python and Ruby treat subprograms as data (so they don't need separate pointer types).
- Ada 95 has pointers to subprograms. Java does not.

## 9.8 Design Issues for Functions
### 9.8.1 Functional Side Effects
Function parameters **should** be in-mode to avoid the side-effect problems of §7.2.2.1. Ada functions are restricted to in-mode formals. Most imperative languages still allow inout/by-reference function parameters, so side effects are possible. Pure functional languages (Haskell) have no mutable variables → no side effects.

### 9.8.2 Types of Returned Values
- C: any type except arrays and functions (handled via pointers).
- C++: above plus user-defined classes.
- **Ada, Python, Ruby**: any type (but Ada functions aren't first-class — can't be returned themselves).
- In **first-class** subprograms (Python, Ruby, most functional languages), subprograms can be passed, returned, and assigned to variables.
- Java/C# have no top-level functions, but methods can return any type or class.

### 9.8.3 Number of Returned Values
Most languages: one. Ruby: multiple — a bare `return` returns `nil`, one expression returns the value, multiple expressions return an array. ML/F#/Python: pack multiple results in a **tuple**.

## 9.9 Overloaded Subprograms
An **overloaded subprogram** has the same name as another in the same referencing environment. Each version must have a **unique protocol** (parameters differ in number/order/types, possibly return type). C++/Java/C# have many predefined overloaded constructors and methods, and let users define their own.

**Coercions complicate disambiguation**: if no method matches exactly but several match via different coercion paths, which is chosen? Each language must define a ranking; C++'s rules are notably complex.

Return type alone **cannot** disambiguate in C++/Java/C# because the call's context isn't enough to determine which return type is wanted. Combining overloading with default parameters can also produce ambiguous calls: with `void fun(float b = 0.0)` and `void fun()` both present, the call `fun()` is ambiguous (compile error).

## 9.10 Generic Subprograms
A **polymorphic subprogram** takes parameters of different types on different activations.
- **Ad hoc polymorphism**: overloaded subprograms (versions can behave differently).
- **Subtype polymorphism**: a `T`-typed variable can hold any subtype of `T` (OO languages).
- **Generic / parametric polymorphism**: one subprogram with explicit type parameters; all instantiations behave the same. Provided in C++, Java 5.0+, C# 2005+, F#.
- Python and Ruby provide a more general dynamic polymorphism because variables have no types.

### 9.10.1 Generic Functions in C++ (Template Functions)
```cpp
template <class Type>
Type max(Type first, Type second) {
  return first > second ? first : second;
}
```
- `class identifier` and `typename identifier` are valid template parameter forms; `typename` is also used to pass values (e.g. an integer array size).
- Templates are **instantiated implicitly** when the function is named in a call or its address is taken. **A separate copy of the code is generated for each instantiation** (different types).
- Templates avoid the pitfalls of macros: `#define max(a,b) ((a)>(b)?(a):(b))` evaluates `a` twice and breaks on `max(x++, y)` (`x` incremented twice on the larger-branch).

### 9.10.2 Generic Methods in Java 5.0
Syntax: `generic_class<T>`, `public static <T> T doIt(T[] list)`. Differences from C++:
- Generic parameters must be **classes**, not primitives.
- **Only one copy of the code** is generated; the internal version (called a **raw method**) operates on `Object` and the compiler inserts casts at use sites.
- **Bounds** restrict the generic parameter to subtypes of a given class/interface: `<T extends Comparable>` — `T` must extend `Comparable` (or implement the interface if it's an interface). Multiple bounds are joined with `&`.
- **Wildcard types**: `Collection<?>` matches any collection of any element type. Restricted wildcard: `ArrayList<? extends Shape>` allows any list whose elements extend `Shape`.

### 9.10.3 Generic Methods in C# 2005
Similar to Java 5.0 but **no wildcard types**. Unique feature: if the compiler can infer the generic parameter from the actual, the parameter can be omitted: `MyClass.DoIt(17)` infers `DoIt<int>`.

### 9.10.4 Generic Functions in F#
F#'s type inference assigns generic types when it cannot determine a concrete type — this is **automatic generalization**. Apostrophe prefixes a generic type variable: `'a`. Type-constrained operations (like arithmetic) prevent generalization. F# generic functions are less useful than C++/Java/C# generics because F# has no type coercion, so a generic numeric function won't accept both ints and floats.

## 9.11 User-Defined Overloaded Operators
Allowed in Ada, C++, Python, Ruby. In **Python**, binary operators are method calls: `x + y` is `x.__add__(y)`. Overload `+` on a `Complex` class by writing `def __add__(self, second): return Complex(self.real + second.real, self.imag + second.imag)`. `self` is the explicit receiver. In C++, write `Complex operator+(Complex &second) { ... }`.

## 9.12 Closures
A **closure** = a subprogram **together with its referencing environment** at definition.

- Needed when (1) the language allows nested subprograms, (2) static scoping is used, (3) subprograms can be passed as parameters or stored in variables — i.e. can be called from outside their enclosing subprogram.
- Variables in enclosing subprograms must outlive the enclosing subprogram's activation — they need **unlimited extent** (effectively heap-dynamic).
- Languages with closures: nearly all functional languages, most scripting languages, and C# (among primarily imperative).

JavaScript example:
```javascript
function makeAdder(x) {
  return function(y) { return x + y; }
}
var add10 = makeAdder(10);
var add5  = makeAdder(5);
add10(20);  // 30
add5(20);   // 25
```
Each call to `makeAdder` produces a different anonymous function bound to its own `x`. The lifetime of each `x` extends as long as the produced function is reachable.

In C#, the same pattern uses nested **anonymous delegates** or **lambda expressions**: `return delegate(int y) { return x + y; }` or, more concisely, `return y => x + y`. Ruby blocks are closures.

## 9.13 Coroutines
A **coroutine** is a subprogram with **multiple entry points**, in a **symmetric** (peer-to-peer) relationship rather than master/slave.
- Invocation is called **resume**, not call.
- Coroutines maintain state between activations (must be history-sensitive — therefore have **static** locals).
- On a subsequent resume, execution continues just past the last `resume` it issued.
- Only one coroutine is in execution at a time — this is **quasi-concurrency**.
- Coroutines are typically created by a **master unit** that resumes one of them to start the dance.
- Example application: a card-game simulation with one coroutine per player passing turns.
- Python **generators** are a form of coroutine (Sebesta footnote: 9.13.13).

---

<a id="project-mapping"></a>
# Project Mapping (Sebesta → Part 2 sections)

These four chapters are the primary source material for Part 2 of the DSL project. The exact section labels depend on your handout — the table below maps each deliverable topic to the Sebesta sections it should cite.

| Deliverable topic | Sebesta sections to cite |
|---|---|
| **Data types section** — primitive types, structured types, type checking, equivalence, lifetimes (already covered partially in D1 §4.6 via Ch 5). | **§6.1–6.16** as relevant: §6.2 primitives, §6.3 strings, §6.4 enumerations, §6.5 arrays (including §6.5.3 array categories that map onto your "lifetime category" choice from Ch 5), §6.6 associative arrays, §6.7 records, §6.8 tuples, §6.9 lists, §6.10 unions, §6.11 pointers/references, §6.12 optional types, §6.13 type checking, §6.14 strong typing, §6.15 type equivalence (name vs structure), §6.16 theory. |
| **Expressions section** — what the DSL's expressions are and how they evaluate. | **§7.2.1.1** precedence, **§7.2.1.2** associativity (recall: in EBNF the parser enforces associativity; if you use left-recursive BNF, it's enforced by the grammar — see §3.3.1.8), **§7.2.1.3** parentheses, **§7.2.2** operand evaluation order + functional side effects + referential transparency, **§7.3** overloaded operators, **§7.4** type conversions (widening/narrowing, coercion vs cast), **§7.5** relational/Boolean expressions, **§7.6** short-circuit evaluation. |
| **Assignment statement section** — what assignment(s) the DSL provides. | **§7.7.1** simple assignments, **§7.7.2** conditional targets, **§7.7.3** compound assignment operators, **§7.7.4** unary assignment, **§7.7.5** assignment as expression (and the resulting hazards), **§7.7.6** multiple assignment, **§7.7.7** functional-style assignment (`val`/`let`), **§7.8** mixed-mode assignment. |
| **Control structures section** — selection and iteration. | **§8.2.1** two-way selection (including the dangling-else resolution your DSL uses), **§8.2.2** multiple selection (if you have a `switch`/`case`-style — choose your design from §8.2.2.2 alternatives and justify), **§8.3.1** counter-controlled loops, **§8.3.2** logical loops (pretest/posttest), **§8.3.3** user-located exits, **§8.3.4** data-based iteration / foreach, **§8.4** unconditional branching (if any), **§8.5** guarded commands (only if your DSL uses Dijkstra-style nondeterminism). |
| **Subprograms section** — what your DSL's procedures/functions look like. | **§9.2.2** definitions/protocols, **§9.2.3** parameters (positional/keyword/default/variable), **§9.2.4** procedure vs function, **§9.3** design issues, **§9.4.1** locals static vs stack-dynamic (and link back to §5.4.3 lifetimes), **§9.4.2** nested subprograms, **§9.5** parameter passing — for the chosen mode(s) cite §9.5.2.1–§9.5.2.4 by name (don't say "pass-by-reference" without §9.5.2.4), **§9.5.5** type checking parameters, **§9.5.7** design considerations, **§9.8** function design issues, and §9.9–9.12 only if your DSL has overloaded/generic/closure features. |
| **Semantics section (D1 §4.4 / Part 2)** — formal semantics of two non-trivial constructs. | Use the **operational** or **denotational** style from **§3.5.1 / §3.5.2** (Ch 3, already in the Part 1 guide). Pull state-changing semantics from §7.7 (assignment), and the relevant control semantics from §8.2 / §8.3. Axiomatic semantics is **not** accepted (per the handout). |

---

<a id="cheatsheet"></a>
# Quick Vocabulary Cheat-Sheet
Terms the grader and exam will expect to see used precisely.

**Chapter 6 (Data Types)**
- **primitive type / structured type / aggregate**
- **descriptor** (compile-time vs run-time)
- **type operator / type constructor**
- **precision / range** (floating-point)
- **IEEE 754 / single & double precision**
- **BCD** (binary coded decimal)
- **ASCII / Unicode / UCS-2 / UCS-4 / UTF-32**
- **static-length / limited dynamic length / dynamic length** (strings)
- **null-terminated string**
- **enumeration constant**
- **array subscript / index / element type / subscript type**
- **static array / fixed stack-dynamic / fixed heap-dynamic / heap-dynamic** (four array categories)
- **sparse array**
- **rectangular vs jagged array**
- **slice**
- **row major / column major order**
- **access function** (for arrays)
- **associative array / hash / dictionary**
- **record / field / level number** (COBOL)
- **fully qualified reference / elliptical reference**
- **field offset**
- **tuple**
- **list / CAR / CDR / CONS / list comprehension**
- **union / free union / discriminated union / tag / discriminant**
- **pointer / reference / dereferencing / nil**
- **anonymous variable / heap-dynamic variable**
- **dangling pointer / dangling reference**
- **lost heap-dynamic variable / garbage / memory leak**
- **tombstone / locks-and-keys** (dangling-pointer solutions)
- **reference counter / mark-sweep / eager vs lazy / incremental mark-sweep**
- **optional type / null / nil**
- **type checking / coercion / type error / compatible type**
- **static vs dynamic type checking**
- **strongly typed**
- **type equivalence: name vs structure**
- **derived type vs subtype** (Ada)
- **type map / type system**
- **Cartesian product / finite mapping / set union** (theory)

**Chapter 7 (Expressions and Assignment)**
- **infix / prefix / postfix; unary / binary / ternary**
- **identity operator** (unary +)
- **precedence / associativity** (left, right, nonassociative)
- **operand evaluation order**
- **functional side effect**
- **referential transparency**
- **overloaded operator** (predefined and user-defined)
- **narrowing / widening conversion**
- **coercion vs cast**
- **mixed-mode expression / mixed-mode assignment**
- **relational operator / Boolean operator**
- **strict equality (===)** (JavaScript / PHP)
- **short-circuit evaluation**
- **compound assignment operator / unary assignment operator**
- **assignment as expression / multiple assignment**

**Chapter 8 (Statement-Level Control Structures)**
- **control structure / control statement**
- **selection statement (two-way / multiple)**
- **dangling else**
- **selector expression / unit type ()**
- **fall-through / break-terminated segment**
- **else-if / elif / COND**
- **iterative statement / loop body**
- **loop variable / loop parameters / stepsize**
- **pretest vs posttest**
- **counter-controlled / logically-controlled / user-located** loops
- **labeled break / last / continue**
- **iterator / foreach / generator / yield**
- **goto / unconditional branch**
- **guarded command / guard / fatbar [] / nondeterministic choice**

**Chapter 9 (Subprograms)**
- **subprogram definition / call / header / body**
- **active subprogram**
- **parameter profile / protocol**
- **prototype** (C/C++)
- **formal parameter / actual parameter**
- **positional / keyword / default / variable parameters**
- **array formal parameter** (Ruby)
- **procedure vs function**
- **stack-dynamic vs static local variables / history sensitivity**
- **nested subprograms**
- **in mode / out mode / inout mode** (semantics models)
- **pass-by-value / pass-by-result / pass-by-value-result / pass-by-copy / pass-by-reference / pass-by-name**
- **constant reference parameter** (C++ `const &`)
- **thunk** (ALGOL 60 pass-by-name closure)
- **pass-by-assignment / pass-by-sharing** (Python, Ruby)
- **type checking parameters / ref must match exactly** (C#)
- **subprogram as parameter / shallow / deep / ad hoc binding**
- **function pointer** (C/C++) / **delegate / multicast delegate** (C#)
- **first-class subprogram**
- **overloaded subprogram**
- **polymorphism: ad hoc / subtype / parametric (generic)**
- **template function / raw method / bound / wildcard type / bounded wildcard**
- **automatic generalization** (F#)
- **user-defined operator overloading** (`__add__`, `operator+`)
- **closure / unlimited extent**
- **lambda expression / anonymous delegate**
- **coroutine / resume / quasi-concurrency / symmetric unit control / generator**

---

*End of guide. If a topic appears in this file but the agent is unsure of a detail, it must search the book before answering — the book is the source of truth.*
