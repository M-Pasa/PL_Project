Source: claude.ai web interface (chat with Sebesta chapter PDFs in project knowledge).
Date: 02-05-2026
Model: Claude web app, Opus 4.7.

================================================================================
So far we have covered 7 chapters and now I got a project in this course, the
project aims to design a programming language (DSL), and the design proecess
and the architecture will be aligned with the related chapters from the book:
Chapters 1,3,4,5.
Write me an MD file that summarize what is mentioned in the book in the listed
chapters in this matter. This file will be used as a guide by my AI agent.

================================================================================
CLAUDE (response, summary form — full markdown deliverable saved separately as
docs/CSE341_Project_Guide_Ch1_3_4_5.md)
================================================================================
Boss, I've gone through chapters 1, 3, 4, and 5 of Sebesta carefully and pulled
out everything that maps to your Part 1 deliverables (Language Overview,
Lexical Structure, Syntax, Scope/Binding/Lifetime). Let me create the guide now.

[Tool use: Created a file, read a file]

Here's your guide, Boss. Here's what it does and how it's organized:

Structure — Four chapter sections (1, 3, 4, 5) following Sebesta's own section
numbering, plus a final Project Mapping table that tells the agent exactly
which book section to cite for each Part 1 D1 deliverable section
(§4.1 Language Overview → Ch. 1; §4.2 Lexical Structure → §4.2 + §3.2;
§4.3 Syntax → §3.3; §4.6 Names/Binding/Scope/Lifetime → all of Ch. 5).

What I emphasized for the agent:
- The four evaluation criteria with their characteristics tables — needed
  for D1 §4.1 and §4.8 trade-offs.
- BNF vs. EBNF mechanics, with the precedence-via-layered-nonterminals
  pattern and the left-recursion-forces-left-associativity rule (which trips
  people up in EBNF).
- The lexer construction approaches and the state-diagram + character-class
  technique.
- The full LL grammar restrictions (no left recursion, pairwise disjointness,
  left factoring) since recursive descent is the natural choice for the
  project.
- The sextuple of variable attributes and the four lifetime categories
  verbatim from Sebesta — the exam will use these exact names.
- The static-vs-dynamic scoping example and Sebesta's §5.5.7 list of why
  static wins (this answers the handout's "what would break if you
  switched?" requirement directly).

What I deliberately kept brief — Ch. 3's semantics §3.5 (operational /
denotational / axiomatic) since that's a Part 2 concern, and attribute
grammars (background only). The handout explicitly excludes axiomatic, so
I flagged that.

One thing to flag — the handout marks the project as covering "Chapters 1–7"
overall, with Part 1 specifically tied to Ch. 1, 3, 4, 5. Chapter 2 (Evolution
of Major Languages) isn't on the Part 1 exam, so I left it out of the guide.
If you want me to add a brief Ch. 2 section anyway (it shows up in your
project knowledge), say the word.
