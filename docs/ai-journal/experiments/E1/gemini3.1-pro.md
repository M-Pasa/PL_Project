Session Link: https://gemini.google.com/share/042e38a9cc73

Here is the EBNF (Extended Backus-Naur Form) grammar for FitLang. It captures the structural constraints, scoping, and specific syntax rules outlined for the athlete context, workouts, meals, rules, and scheduling.

FitLang EBNF Grammar
Program         ::= AthleteDecl

(* --- Context --- *)
AthleteDecl     ::= "athlete" Identifier "{" AthleteBody "}"
AthleteBody     ::= BodyweightDecl GoalModeDecl WorkoutDecl+ MealDecl+ RuleDecl* ScheduleDecl

BodyweightDecl  ::= "bodyweight" Quantity
GoalModeDecl    ::= "goal" ( "bulk" | "cut" | "maintain" )

(* --- Workouts & Exercises --- *)
WorkoutDecl     ::= "workout" Identifier "{" ExerciseDecl+ ProgressionRule? "}"
ExerciseDecl    ::= "exercise" Identifier "{" "sets" Number "reps" Number "load" Quantity "}"
ProgressionRule ::= "progression" Quantity "per" TimeUnit

(* --- Nutrition --- *)
MealDecl        ::= "meal" Identifier "{" "protein" Quantity "carbs" Quantity "fat" Quantity "}"

(* --- Rules & Bindings --- *)
RuleDecl        ::= TargetRule | GoalRule | LetBinding
TargetRule      ::= "target" Macro Operator Value [ "per" "kg" ]
GoalRule        ::= "goal_rate" Quantity "per" TimeUnit
LetBinding      ::= "let" Identifier "=" Value

Macro           ::= "protein" | "carbs" | "fat"
Operator        ::= ">" | "<" | ">=" | "<=" | "=="
Value           ::= Quantity | Number | Identifier

(* --- Schedule --- *)
ScheduleDecl    ::= "schedule" "{" IdentifierList "}"
IdentifierList  ::= Identifier { "," Identifier }

(* --- Primitives & Types --- *)
Quantity        ::= Number Unit
Number          ::= Digit+ [ "." Digit+ ]

(* Unit Families *)
Unit            ::= MassUnit | TimeUnit | EnergyUnit
MassUnit        ::= "kg" | "lb" | "g"
TimeUnit        ::= "week" | "day" | "month"
EnergyUnit      ::= "kcal" | "cal" | "J"

(* Lexical Tokens *)
Identifier      ::= Letter { Letter | Digit | "_" }
Letter          ::= "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" 
                  | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T" 
                  | "U" | "V" | "W" | "X" | "Y" | "Z" | "a" | "b" | "c" | "d" 
                  | "e" | "f" | "g" | "h" | "i" | "j" | "k" | "l" | "m" | "n" 
                  | "o" | "p" | "q" | "r" | "s" | "t" | "u" | "v" | "w" | "x" 
                  | "y" | "z"
Digit           ::= "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"



Grammar Design Notes
Strict Ordering: The AthleteBody rule strictly enforces the order requested: Bodyweight/Goal first, followed by at least one Workout (+), at least one Meal (+), optional Rules (*), and concluding with exactly one Schedule.

Context Encapsulation: The entire program is wrapped inside athlete Identifier { ... }, establishing the required single-athlete context.

Unit Safety: While the grammar structurally pairs a Number with a Unit to form a Quantity, enforcing that a specific unit belongs to the correct family (e.g., preventing the use of kg for time) is typically handled by the compiler's semantic analysis phase rather than the raw parser.

Values: The Value rule allows Target rules and Let bindings to accept literal numbers, physical quantities, or references to previously defined Identifiers.

Example FitLang Program