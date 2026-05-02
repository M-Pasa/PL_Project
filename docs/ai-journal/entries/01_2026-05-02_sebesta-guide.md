---
entry: 01
date: 02-05-2026
phase: Design / Writing (preparing reference material)
ai_tool: Claude (claude.ai web interface; Opus 4.7)
---

# Entry 01 — Generating a Sebesta Ch. 1/3/4/5 reference guide

## Goal
Produce a single Markdown file that distils the relevant material from Sebesta's *Concepts of Programming Languages* (Ch. 1, 3, 4, 5 — the chapters tied to Part 1 of this project) so that my AI coding agent (Claude Code, working locally on the project repo) has a concise, citation-ready Sebesta reference without needing to re-open the chapter PDFs every time.

## Prompt
Verbatim, single round (no iteration):

> So far we have covered 7 chapters and now I got a project in this course, the project aims to design a programming language (DSL), and the design proecess and the architecture will be aligned with the related chapters from the book: Chapters 1,3,4,5. Write me an MD file that summarize what is mentioned in the book in the listed chapters in this matter. This file will be used as a guide by my AI agent.

(The Sebesta chapter PDFs were attached as project knowledge in the web chat — the AI had direct access to them.)

## Response (key part)
Claude produced a single MD deliverable (now saved at `docs/CSE341_Project_Guide_Ch1_3_4_5.md`). Its self-described structure:

- Four chapter sections (1, 3, 4, 5) following Sebesta's own section numbering, plus a final **Project Mapping table** that tells the agent which book section to cite for each Part 1 deliverable section (§4.1 → Ch. 1; §4.2 → §4.2 + §3.2; §4.3 → §3.3; §4.6 → all of Ch. 5).
- Emphasis on: the four evaluation criteria; BNF/EBNF mechanics including precedence-via-layered-nonterminals and left-recursion-forces-left-associativity; lexer construction (state diagrams + character classes); LL grammar restrictions (no left recursion, pairwise disjointness, left factoring); the variable sextuple; the four lifetime categories; static vs. dynamic scoping with §5.5.7's "why static wins" list.
- Kept brief deliberately: §3.5 semantics (operational/denotational/axiomatic — flagged as Part 2 concern); attribute grammars (background only). Flagged that axiomatic semantics is excluded per the handout.
- Vocabulary cheat-sheet at the end with the terms-of-art the exam expects (lexeme/token, l-value/r-value, sentential form, FIRST/FOLLOW, the four lifetimes, the sextuple, etc.).

The full transcript (such as it is — single round) is saved at `docs/ai-journal/experiments/01_guide-creation-chat.txt`.

## Accepted
The entire guide, in full. It is now the primary Sebesta reference for FitLang's design work; the agent (Claude Code) has been instructed via persistent memory to consult this file rather than re-opening the chapter PDFs unless the guide is silent on a specific question. The Project Mapping table at the end is particularly load-bearing — it converts the rubric's "Use of Sebesta Framework" dimension into a concrete checklist (which §-numbered citation belongs in each D1 section).

## Rejected / modified
- Claude offered to add a brief Chapter 2 (Evolution of Major Languages) section, given that Ch. 2 was in the project knowledge. **Declined** by omission — Ch. 2 is not on the Part 1 exam, and adding it would have inflated the guide without serving the rubric.
- No content was modified after first generation; one-shot acceptance.

## Errors I caught
At the time of this session, I did not formally audit the guide line-by-line against Sebesta. I am relying on it as a working reference and will flag and correct any discrepancies as they surface during D1 drafting (in particular when writing §4.3 EBNF and §4.6 Names/Binding/Scope, which are the most fact-dense sections of D1). Any errors found will be logged in subsequent journal entries — that is part of why the journal demands ≥3 documented AI errors across the project (handout §6).

One *omission* worth noting up-front rather than waiting: the guide's Ch. 5 coverage centres on the *standard* lifetime categories (static / stack-dynamic / explicit-heap-dynamic / implicit-heap-dynamic) but does not include worked exam-style problems on referencing-environment lookup. That's a gap I'll need to fill from the chapter PDF (or the textbook directly) when preparing my handwritten exam notes.

## Reflection

I have worked with AI agents for a while and I noticed that AI agents preform well with md files so instead of putting the pdf file of the book, I made a guide from about the related chapters from the book for my AI agent to use. 