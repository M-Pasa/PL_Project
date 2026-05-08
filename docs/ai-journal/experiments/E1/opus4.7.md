Session link: https://claude.ai/share/1e2cc5dd-0bf8-45e0-a9d9-14419228ed7e

Here are the EBNF grammar rules for FitLang based on your specification:
(* ===== Top-level program ===== *)
program         = athlete_block ;

athlete_block   = "athlete" identifier "{"
                    bodyweight_decl
                    goal_decl
                    { workout_block }
                    { meal_block }
                    { rule }
                    schedule_stmt
                  "}" ;

bodyweight_decl = "bodyweight" "=" quantity ";" ;
goal_decl       = "goal" "=" goal_mode ";" ;
goal_mode       = "bulk" | "cut" | "maintain" ;

(* ===== Workout blocks ===== *)
workout_block   = "workout" identifier "{"
                    exercise { exercise }
                    [ progression_rule ]
                  "}" ;

exercise        = "exercise" identifier "{"
                    "sets"   "=" integer  ";"
                    "reps"   "=" integer  ";"
                    "load"   "=" quantity ";"
                  "}" ;

progression_rule = "progression" "+=" quantity "per" time_unit ";" ;

(* ===== Meal blocks ===== *)
meal_block      = "meal" identifier "{"
                    macro_decl
                    macro_decl
                    macro_decl
                  "}" ;

macro_decl      = macro_name "=" quantity ";" ;
macro_name      = "protein" | "carbs" | "fat" ;

(* ===== Rules ===== *)
rule            = target_rule
                | goal_rule
                | let_binding ;

target_rule     = "target" macro_name comparator threshold ";" ;
comparator      = ">=" | "<=" | ">" | "<" ;
threshold       = quantity [ "per" "kg" "bodyweight" ]
                | number   [ "per" "kg" "bodyweight" ] ;

goal_rule       = "goal_rate" "=" signed_quantity "per" time_unit ";" ;

let_binding     = "let" identifier "=" ( quantity | number ) ";" ;

(* ===== Schedule ===== *)
schedule_stmt   = "schedule" "{"
                    "workouts" "=" identifier_list ";"
                    "meals"    "=" identifier_list ";"
                  "}" ;

identifier_list = identifier { "," identifier } ;

(* ===== Quantities and units ===== *)
quantity        = number unit ;
signed_quantity = [ "+" | "-" ] number unit ;

unit            = mass_unit | energy_unit | time_unit ;
mass_unit       = "kg" | "g" | "lb" | "oz" ;
energy_unit     = "kcal" | "kJ" ;
time_unit       = "sec" | "min" | "hour" | "day" | "week" | "month" ;

(* ===== Lexical primitives ===== *)
number          = digit { digit } [ "." digit { digit } ] ;
integer         = digit { digit } ;
identifier      = letter { letter | digit | "_" } ;
letter          = "A".."Z" | "a".."z" ;
digit           = "0".."9" ;



A few design notes
Unit family safety. The grammar separates mass_unit, energy_unit, and time_unit as distinct nonterminals, but it does not enforce which family applies where — for instance, bodyweight = 5 kcal; is grammatically valid here. That kind of constraint belongs in semantic analysis (a type checker over unit families), not in EBNF, because context-free grammars can't express "this quantity must belong to the mass family." Your specification mentions mixing families is an error, so I'd recommend handling that in a later type-checking pass.
Progression and goal rate. I used += for progression to make the "increase" semantics explicit, and a signed quantity for goal_rate so users can express both bulking (+0.25 kg per week) and cutting (-0.5 kg per week).
Per-bodyweight scaling. The threshold rule allows 2 g per kg bodyweight for protein targets, which is the standard way nutrition programs express macros relative to bodyweight.
One athlete per program. The spec says "every FitLang program describes a single athlete," so program reduces directly to a single athlete_block.
A small grammar suggestion: you may want to require bodyweight_decl and goal_decl to appear in either order rather than fixed — if so, that's a small change to use a permutation pattern.
