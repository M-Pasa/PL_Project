package fitlang;

import fitlang.ast.Ast;
import java.util.*;

/**
 * FitLang recursive-descent parser (LL(1)).
 * One method per nonterminal; method names mirror the §4.3 EBNF productions.
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    // ── Navigation ───────────────────────────────────────────────────────────

    private Token peek()               { return tokens.get(pos); }
    private Token advance()            { return tokens.get(pos++); }
    private boolean check(Token.Type t){ return peek().type == t; }

    private Token expect(Token.Type t) {
        Token tok = peek();
        if (tok.type != t)
            throw new FitLangException(
                "Expected " + t + " but got " + tok.type + " ('" + tok.lexeme + "')",
                tok.line, tok.col);
        return advance();
    }

    // ── Unit helpers ─────────────────────────────────────────────────────────

    private boolean isUnit(Token.Type t) {
        switch (t) {
            case G: case KG: case LB:
            case KCAL: case KJ:
            case S: case MIN: case H: case DAY: case WEEK:
                return true;
            default:
                return false;
        }
    }

    /** Consume the next token if it is a unit; throw otherwise. */
    private Token expectUnit() {
        Token tok = peek();
        if (!isUnit(tok.type))
            throw new FitLangException(
                "Expected a unit (g/kg/lb/kcal/kJ/s/min/h/day/week) but got '" + tok.lexeme + "'",
                tok.line, tok.col);
        return advance();
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    /** Parse the entire source; return the top-level AST node. */
    public Ast.AthleteBlock parse() {
        Ast.AthleteBlock block = parseAthleteBlock();
        expect(Token.Type.EOF);
        return block;
    }

    // ── Productions ──────────────────────────────────────────────────────────

    // <program> → <athlete-block>
    // <athlete-block> → "athlete" "(" <athlete-params> ")" "{"
    //                       { <routine-decl> }
    //                       <body-sections> <rules-section> <schedule-stmt>
    //                   "}"
    private Ast.AthleteBlock parseAthleteBlock() {
        expect(Token.Type.ATHLETE);
        expect(Token.Type.LPAREN);
        Ast.AthleteParams params = parseAthleteParams();
        expect(Token.Type.RPAREN);
        expect(Token.Type.LBRACE);

        // Routine declarations (athlete scope, before workouts)
        List<Ast.RoutineDecl> routines = new ArrayList<>();
        while (check(Token.Type.ROUTINE)) routines.add(parseRoutineDecl());

        // §4.7 let-section — bindings visible in workout/meal bodies and rules.
        List<Ast.LetDecl> lets = new ArrayList<>();
        while (check(Token.Type.LET)) lets.add(parseLetDecl());

        // <body-sections> → <plan-section> [ <intake-section> ] | <intake-section>
        List<Ast.WorkoutDecl> workouts = new ArrayList<>();
        List<Ast.MealDecl>    meals    = new ArrayList<>();

        if (check(Token.Type.WORKOUT)) {
            do { workouts.add(parseWorkoutDecl()); } while (check(Token.Type.WORKOUT));
            while (check(Token.Type.MEAL)) meals.add(parseMealDecl());
        } else if (check(Token.Type.MEAL)) {
            do { meals.add(parseMealDecl()); } while (check(Token.Type.MEAL));
        } else {
            Token t = peek();
            throw new FitLangException(
                "Expected 'workout' or 'meal' block inside athlete block", t.line, t.col);
        }

        // <rules-section> → { <rule-decl> }
        List<Ast.RuleDecl> rules = new ArrayList<>();
        while (isRuleStart(peek().type)) {
            rules.add(parseRuleDecl());
        }

        Ast.ScheduleStmt schedule = parseScheduleStmt();
        expect(Token.Type.RBRACE);

        return new Ast.AthleteBlock(params, routines, lets, workouts, meals, rules, schedule);
    }

    private boolean isRuleStart(Token.Type t) {
        return t == Token.Type.TARGET
            || t == Token.Type.GOAL
            || t == Token.Type.WHEN;
    }

    // <athlete-params> → "bodyweight" ":" <quantity> "," "goal" ":" <goal-mode>
    private Ast.AthleteParams parseAthleteParams() {
        expect(Token.Type.BODYWEIGHT);
        expect(Token.Type.COLON);
        Ast.Quantity bw = parseQuantity();
        expect(Token.Type.COMMA);
        expect(Token.Type.GOAL);
        expect(Token.Type.COLON);
        Ast.GoalMode mode = parseGoalMode();
        return new Ast.AthleteParams(bw, mode);
    }

    // <goal-mode> → "bulk" | "cut" | "maintain"
    private Ast.GoalMode parseGoalMode() {
        Token t = peek();
        switch (t.type) {
            case BULK:     advance(); return Ast.GoalMode.BULK;
            case CUT:      advance(); return Ast.GoalMode.CUT;
            case MAINTAIN: advance(); return Ast.GoalMode.MAINTAIN;
            default:
                throw new FitLangException(
                    "Expected goal mode: bulk, cut, or maintain", t.line, t.col);
        }
    }

    // <workout-decl> → "workout" STRING_LIT "{" <workout-item>+ [ <progress-stmt> ] "}"
    // <workout-item> → <exercise-decl> | <routine-call>
    private Ast.WorkoutDecl parseWorkoutDecl() {
        expect(Token.Type.WORKOUT);
        String label = expect(Token.Type.STRING_LIT).lexeme;
        expect(Token.Type.LBRACE);

        if (!check(Token.Type.EXERCISE) && !check(Token.Type.USE)) {
            Token t = peek();
            throw new FitLangException(
                "Workout \"" + label + "\" must contain at least one exercise or routine call",
                t.line, t.col);
        }
        List<Ast.WorkoutItem> items = new ArrayList<>();
        while (check(Token.Type.EXERCISE) || check(Token.Type.USE)) {
            if (check(Token.Type.EXERCISE)) items.add(parseExerciseDecl());
            else                            items.add(parseRoutineCall());
        }

        Ast.ProgressStmt progress = null;
        if (check(Token.Type.PROGRESS)) progress = parseProgressStmt();

        expect(Token.Type.RBRACE);
        return new Ast.WorkoutDecl(label, items, progress);
    }

    // <routine-decl> → "routine" STRING_LIT "(" <routine-params> ")"
    //                  "{" <exercise-decl>+ [ <progress-stmt> ] "}"
    private Ast.RoutineDecl parseRoutineDecl() {
        expect(Token.Type.ROUTINE);
        String label = expect(Token.Type.STRING_LIT).lexeme;
        expect(Token.Type.LPAREN);
        List<Ast.RoutineParam> params = parseRoutineParams();
        expect(Token.Type.RPAREN);
        expect(Token.Type.LBRACE);

        if (!check(Token.Type.EXERCISE)) {
            Token t = peek();
            throw new FitLangException(
                "Routine \"" + label + "\" must contain at least one exercise", t.line, t.col);
        }
        List<Ast.ExerciseDecl> exercises = new ArrayList<>();
        do { exercises.add(parseExerciseDecl()); } while (check(Token.Type.EXERCISE));

        Ast.ProgressStmt progress = null;
        if (check(Token.Type.PROGRESS)) progress = parseProgressStmt();

        expect(Token.Type.RBRACE);
        return new Ast.RoutineDecl(label, params, exercises, progress);
    }

    // <routine-params> → IDENT ":" UNIT { "," IDENT ":" UNIT }
    private List<Ast.RoutineParam> parseRoutineParams() {
        List<Ast.RoutineParam> params = new ArrayList<>();
        params.add(parseOneRoutineParam());
        while (check(Token.Type.COMMA)) {
            advance();
            params.add(parseOneRoutineParam());
        }
        return params;
    }

    private Ast.RoutineParam parseOneRoutineParam() {
        Token nameTok = expect(Token.Type.IDENT);
        expect(Token.Type.COLON);
        Token unitTok = expectUnit();
        return new Ast.RoutineParam(
            nameTok.lexeme, unitTok.lexeme, Ast.familyOf(unitTok.type));
    }

    // <routine-call> → "use" STRING_LIT "(" <expression> { "," <expression> } ")"
    private Ast.RoutineCall parseRoutineCall() {
        expect(Token.Type.USE);
        String label = expect(Token.Type.STRING_LIT).lexeme;
        expect(Token.Type.LPAREN);
        List<Ast.Expr> args = new ArrayList<>();
        args.add(parseExpression());
        while (check(Token.Type.COMMA)) {
            advance();
            args.add(parseExpression());
        }
        expect(Token.Type.RPAREN);
        return new Ast.RoutineCall(label, args);
    }

    // <exercise-decl> → "exercise" STRING_LIT "{"
    //                       "sets" ":" NUM_LIT ","
    //                       "reps" ":" NUM_LIT ","
    //                       "weight" ":" <expression>
    //                   "}"
    private Ast.ExerciseDecl parseExerciseDecl() {
        expect(Token.Type.EXERCISE);
        String label = expect(Token.Type.STRING_LIT).lexeme;
        expect(Token.Type.LBRACE);

        expect(Token.Type.SETS);
        expect(Token.Type.COLON);
        Token setsTok = expect(Token.Type.NUM_LIT);

        expect(Token.Type.COMMA);

        expect(Token.Type.REPS);
        expect(Token.Type.COLON);
        Token repsTok = expect(Token.Type.NUM_LIT);

        expect(Token.Type.COMMA);

        expect(Token.Type.WEIGHT);
        expect(Token.Type.COLON);
        Ast.Expr weight = parseExpression();

        expect(Token.Type.RBRACE);

        return new Ast.ExerciseDecl(label,
            parsePositiveInt(setsTok),
            parsePositiveInt(repsTok),
            weight);
    }

    private int parsePositiveInt(Token numTok) {
        double d = Double.parseDouble(numTok.lexeme);
        if (d != Math.floor(d) || d < 1)
            throw new FitLangException(
                "Expected a positive integer, got: " + numTok.lexeme, numTok.line, numTok.col);
        return (int) d;
    }

    // <progress-stmt> → "progress" "weight" <expression>
    private Ast.ProgressStmt parseProgressStmt() {
        expect(Token.Type.PROGRESS);
        expect(Token.Type.WEIGHT);
        return new Ast.ProgressStmt(parseExpression());
    }

    // <meal-decl> → "meal" STRING_LIT "{" <macro-decl>+ "}"
    private Ast.MealDecl parseMealDecl() {
        expect(Token.Type.MEAL);
        String label = expect(Token.Type.STRING_LIT).lexeme;
        expect(Token.Type.LBRACE);

        if (!isMacroToken(peek().type)) {
            Token t = peek();
            throw new FitLangException(
                "Meal \"" + label + "\" must contain at least one macro declaration (protein/carbs/fat)",
                t.line, t.col);
        }
        List<Ast.MacroDecl> macros = new ArrayList<>();
        do { macros.add(parseMacroDecl()); } while (isMacroToken(peek().type));

        expect(Token.Type.RBRACE);
        return new Ast.MealDecl(label, macros);
    }

    private boolean isMacroToken(Token.Type t) {
        return t == Token.Type.PROTEIN || t == Token.Type.CARBS || t == Token.Type.FAT;
    }

    // <macro-decl> → <macro-name> ":" <expression>
    // <macro-name>  → "protein" | "carbs" | "fat"
    private Ast.MacroDecl parseMacroDecl() {
        Token t = peek();
        Ast.MacroName name;
        switch (t.type) {
            case PROTEIN: advance(); name = Ast.MacroName.PROTEIN; break;
            case CARBS:   advance(); name = Ast.MacroName.CARBS;   break;
            case FAT:     advance(); name = Ast.MacroName.FAT;     break;
            default:
                throw new FitLangException("Expected macro name (protein/carbs/fat)", t.line, t.col);
        }
        expect(Token.Type.COLON);
        return new Ast.MacroDecl(name, parseExpression());
    }

    // <rule-decl> → <target-decl> | <goal-rate-decl> | <when-decl>
    // (§4.7: <let-decl> lives in the prefix let-section, not the rules section.)
    private Ast.RuleDecl parseRuleDecl() {
        switch (peek().type) {
            case TARGET: return parseTargetDecl();
            case GOAL:   return parseGoalRateDecl();
            case WHEN:   return parseWhenDecl();
            default:
                Token t = peek();
                throw new FitLangException(
                    "Expected rule declaration (target/goal/when)", t.line, t.col);
        }
    }

    // <when-decl> → "when" "goal" ":" <goal-mode>
    //                   "{" { <rule-decl> } "}"
    //                   [ "else" "{" { <rule-decl> } "}" ]
    private Ast.WhenDecl parseWhenDecl() {
        expect(Token.Type.WHEN);
        expect(Token.Type.GOAL);
        expect(Token.Type.COLON);
        Ast.GoalMode mode = parseGoalMode();
        expect(Token.Type.LBRACE);
        List<Ast.RuleDecl> thenRules = new ArrayList<>();
        while (isRuleStart(peek().type)) thenRules.add(parseRuleDecl());
        expect(Token.Type.RBRACE);

        List<Ast.RuleDecl> elseRules = null;
        if (check(Token.Type.ELSE)) {
            advance();
            expect(Token.Type.LBRACE);
            elseRules = new ArrayList<>();
            while (isRuleStart(peek().type)) elseRules.add(parseRuleDecl());
            expect(Token.Type.RBRACE);
        }
        return new Ast.WhenDecl(mode, thenRules, elseRules);
    }

    // <target-decl> → "target" <macro-name> <rel-op> <expression>
    //                     [ "per" UNIT "of" "bodyweight" ]
    private Ast.TargetDecl parseTargetDecl() {
        expect(Token.Type.TARGET);
        Token macroTok = peek();
        Ast.MacroName macro;
        switch (macroTok.type) {
            case PROTEIN: advance(); macro = Ast.MacroName.PROTEIN; break;
            case CARBS:   advance(); macro = Ast.MacroName.CARBS;   break;
            case FAT:     advance(); macro = Ast.MacroName.FAT;     break;
            default:
                throw new FitLangException(
                    "Expected macro name (protein/carbs/fat) after 'target'", macroTok.line, macroTok.col);
        }
        String relOp = parseRelOp();
        Ast.Expr qty = parseExpression();

        String perUnit = null;
        Ast.UnitFamily perFamily = null;
        if (check(Token.Type.PER)) {
            advance();
            Token unitTok = expectUnit();
            perUnit   = unitTok.lexeme;
            perFamily = Ast.familyOf(unitTok.type);
            expect(Token.Type.OF);
            expect(Token.Type.BODYWEIGHT);
        }
        return new Ast.TargetDecl(macro, relOp, qty, perUnit, perFamily);
    }

    private String parseRelOp() {
        Token t = peek();
        switch (t.type) {
            case GTE:  advance(); return ">=";
            case LTE:  advance(); return "<=";
            case GT:   advance(); return ">";
            case LT:   advance(); return "<";
            case EQEQ: advance(); return "==";
            case NEQ:  advance(); return "!=";
            default:
                throw new FitLangException(
                    "Expected relational operator (>= <= > < == !=)", t.line, t.col);
        }
    }

    // <goal-rate-decl> → "goal" <direction> <expression>
    // <direction>       → "lose" | "gain"
    private Ast.GoalRateDecl parseGoalRateDecl() {
        expect(Token.Type.GOAL);
        Token t = peek();
        Ast.Direction dir;
        switch (t.type) {
            case LOSE: advance(); dir = Ast.Direction.LOSE; break;
            case GAIN: advance(); dir = Ast.Direction.GAIN; break;
            default:
                throw new FitLangException(
                    "Expected direction 'lose' or 'gain' after 'goal'", t.line, t.col);
        }
        return new Ast.GoalRateDecl(dir, parseExpression());
    }

    // <let-decl> → "let" IDENT "=" <expression>
    private Ast.LetDecl parseLetDecl() {
        expect(Token.Type.LET);
        String name = expect(Token.Type.IDENT).lexeme;
        expect(Token.Type.ASSIGN);
        return new Ast.LetDecl(name, parseExpression());
    }

    // <schedule-stmt> → "plan" [ NUM_LIT ] "week" "with" <schedule-clauses>
    private Ast.ScheduleStmt parseScheduleStmt() {
        expect(Token.Type.PLAN);
        int weeks = 1;
        if (check(Token.Type.NUM_LIT)) {
            weeks = parsePositiveInt(advance());
        }
        expect(Token.Type.WEEK);
        expect(Token.Type.WITH);
        return parseScheduleClauses(weeks);
    }

    // <schedule-clauses>
    //   → "workouts" ":" <name-list> [ "," "meals" ":" <name-list> ]
    //   | "meals"    ":" <name-list>
    private Ast.ScheduleStmt parseScheduleClauses(int weeks) {
        List<String> workouts = null;
        List<String> meals    = null;

        if (check(Token.Type.WORKOUTS)) {
            advance();
            expect(Token.Type.COLON);
            workouts = parseNameList();
            if (check(Token.Type.COMMA)) {
                advance();
                expect(Token.Type.MEALS);
                expect(Token.Type.COLON);
                meals = parseNameList();
            }
        } else if (check(Token.Type.MEALS)) {
            advance();
            expect(Token.Type.COLON);
            meals = parseNameList();
        } else {
            Token t = peek();
            throw new FitLangException(
                "Expected 'workouts' or 'meals' clause in plan statement", t.line, t.col);
        }
        return new Ast.ScheduleStmt(weeks, workouts, meals);
    }

    // <name-list> → "[" STRING_LIT { "," STRING_LIT } "]"
    private List<String> parseNameList() {
        expect(Token.Type.LBRACKET);
        List<String> names = new ArrayList<>();
        names.add(expect(Token.Type.STRING_LIT).lexeme);
        while (check(Token.Type.COMMA)) {
            advance();
            names.add(expect(Token.Type.STRING_LIT).lexeme);
        }
        expect(Token.Type.RBRACKET);
        return names;
    }

    // <quantity> → NUM_LIT UNIT
    private Ast.Quantity parseQuantity() {
        Token numTok  = expect(Token.Type.NUM_LIT);
        Token unitTok = expectUnit();
        return new Ast.Quantity(
            Double.parseDouble(numTok.lexeme),
            unitTok.lexeme,
            Ast.familyOf(unitTok.type));
    }

    // <expression> → <add-expr>                                    (§4.7.1)
    // <add-expr>   → <mul-expr> { ("+" | "-") <mul-expr> }
    // <mul-expr>   → <unary>    { ("*" | "/") <unary>    }
    // <unary>      → [ "-" ] <atom>
    // <atom>       → NUM_LIT [ UNIT [ "/" UNIT ] ]                  // scalar | quantity | rate
    //              | IDENT
    //              | "(" <expression> ")"
    //
    // Rate/divide disambiguation lives inside parseAtom: once we've consumed
    // NUM_LIT UNIT, we peek two tokens — if (SLASH, UNIT), it's a rate atom;
    // otherwise the SLASH belongs to the enclosing <mul-expr> as binary `/`.

    private Ast.Expr parseExpression() { return parseAddExpr(); }

    private Ast.Expr parseAddExpr() {
        Ast.Expr left = parseMulExpr();
        while (check(Token.Type.PLUS) || check(Token.Type.MINUS)) {
            String op    = advance().lexeme;
            Ast.Expr right = parseMulExpr();
            left = new Ast.BinaryExpr(left, op, right);
        }
        return left;
    }

    private Ast.Expr parseMulExpr() {
        Ast.Expr left = parseUnary();
        while (check(Token.Type.STAR) || check(Token.Type.SLASH)) {
            String op    = advance().lexeme;
            Ast.Expr right = parseUnary();
            left = new Ast.BinaryExpr(left, op, right);
        }
        return left;
    }

    private Ast.Expr parseUnary() {
        if (check(Token.Type.MINUS)) {
            advance();
            return new Ast.UnaryExpr("-", parseAtom());
        }
        return parseAtom();
    }

    private Ast.Expr parseAtom() {
        Token t = peek();
        if (t.type == Token.Type.NUM_LIT) {
            advance();
            double value = Double.parseDouble(t.lexeme);
            // Bare number?
            if (!isUnit(peek().type))
                return new Ast.NumExpr(value);
            // Has a unit — quantity or rate.
            Token numerTok = advance();
            // Peek for the rate continuation: SLASH then UNIT.
            boolean isRate = check(Token.Type.SLASH)
                && (pos + 1 < tokens.size())
                && isUnit(tokens.get(pos + 1).type);
            if (isRate) {
                advance(); // consume SLASH
                Token denomTok = advance(); // the UNIT
                return new Ast.RateExpr(new Ast.Rate(
                    value,
                    numerTok.lexeme, Ast.familyOf(numerTok.type),
                    denomTok.lexeme, Ast.familyOf(denomTok.type)));
            }
            return new Ast.QuantityExpr(new Ast.Quantity(
                value, numerTok.lexeme, Ast.familyOf(numerTok.type)));
        }
        if (t.type == Token.Type.IDENT) {
            advance();
            return new Ast.IdentExpr(t.lexeme);
        }
        if (t.type == Token.Type.LPAREN) {
            advance();
            Ast.Expr inner = parseExpression();
            expect(Token.Type.RPAREN);
            return inner;
        }
        throw new FitLangException(
            "Expected expression atom (number, quantity, rate, identifier, or '(' )",
            t.line, t.col);
    }
}
