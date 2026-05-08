Here is a clean EBNF grammar for FitLang:
program =
    athlete_decl,
    "{",
        { workout_block | meal_block | rule },
        schedule_stmt,
    "}" ;

athlete_decl =
    "athlete", quantity, "goal", goal_mode ;

goal_mode =
    "bulk" | "cut" | "maintain" ;


workout_block =
    "workout", identifier, "{",
        exercise_decl,
        { exercise_decl },
        [ progression_rule ],
    "}" ;

exercise_decl =
    "exercise", identifier, "{",
        "sets", integer, ";",
        "reps", integer, ";",
        "load", quantity, ";",
    "}" ;

progression_rule =
    "progression", quantity, "per", time_unit, ";" ;


meal_block =
    "meal", identifier, "{",
        macro_decl,
        macro_decl,
        macro_decl,
    "}" ;

macro_decl =
    macro_name, quantity, ";" ;

macro_name =
    "protein" | "carbs" | "fat" ;


rule =
      target_rule
    | goal_rule
    | let_binding ;

target_rule =
    "target", macro_name, comparison_op, value_expr, ";" ;

comparison_op =
    "above" | "below" | ">=" | "<=" | ">" | "<" ;

goal_rule =
    "goal", "weight_change", quantity, "per", time_unit, ";" ;

let_binding =
    "let", identifier, "=", value_expr, ";" ;


schedule_stmt =
    "schedule", "{",
        "workouts", ":", identifier_list, ";",
        "meals", ":", identifier_list, ";",
    "}" ;

identifier_list =
    identifier, { ",", identifier } ;


value_expr =
      quantity
    | number
    | identifier
    | per_bodyweight_expr ;

per_bodyweight_expr =
    quantity, "per", "kg", "bodyweight" ;


quantity =
    number, unit ;

unit =
      mass_unit
    | energy_unit
    | time_unit ;

mass_unit =
    "kg" | "lb" | "g" ;

energy_unit =
    "kcal" | "cal" ;

time_unit =
    "day" | "week" | "month" ;


identifier =
    letter, { letter | digit | "_" } ;

number =
    integer, [ ".", digit, { digit } ] ;

integer =
    digit, { digit } ;

letter =
    "A" | ... | "Z" | "a" | ... | "z" ;

digit =
    "0" | ... | "9" ;


Semantic constraints not expressible cleanly in EBNF:
- Athlete bodyweight must be a mass quantity.
- Exercise load must be a mass quantity.
- Meal macros must be gram quantities.
- Progression must be mass per time.
- Goal weight_change must be mass per time.
- Units may only be combined within compatible families.
- schedule workouts must reference declared workout names.
- schedule meals must reference declared meal names.
- target rules must reference valid macros.