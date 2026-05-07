package fitlang.ast;

import fitlang.Token;
import java.util.List;

/**
 * All AST node types for FitLang, as inner static classes.
 * Corresponds to the §4.3 EBNF grammar in D1.
 */
public final class Ast {

    // ── Dimensional families (§4.2.6) ────────────────────────────────────────

    public enum UnitFamily { MASS, ENERGY, TIME }

    public static UnitFamily familyOf(Token.Type unitType) {
        switch (unitType) {
            case G:   case KG:  case LB:  return UnitFamily.MASS;
            case KCAL: case KJ:           return UnitFamily.ENERGY;
            case S: case MIN: case H:
            case DAY: case WEEK:          return UnitFamily.TIME;
            default:
                throw new IllegalArgumentException("Not a unit token: " + unitType);
        }
    }

    // ── Domain enums ─────────────────────────────────────────────────────────

    public enum GoalMode  { BULK, CUT, MAINTAIN }
    public enum MacroName { PROTEIN, CARBS, FAT }
    public enum Direction { LOSE, GAIN }

    // ── Value types ──────────────────────────────────────────────────────────

    public static final class Quantity {
        public final double     value;
        public final String     unit;
        public final UnitFamily family;

        public Quantity(double value, String unit, UnitFamily family) {
            this.value  = value;
            this.unit   = unit;
            this.family = family;
        }

        @Override public String toString() { return value + " " + unit; }
    }

    public static final class Rate {
        public final double     value;
        public final String     numerUnit;
        public final UnitFamily numerFamily;
        public final String     denomUnit;
        public final UnitFamily denomFamily;

        public Rate(double value, String numerUnit, UnitFamily numerFamily,
                    String denomUnit, UnitFamily denomFamily) {
            this.value       = value;
            this.numerUnit   = numerUnit;
            this.numerFamily = numerFamily;
            this.denomUnit   = denomUnit;
            this.denomFamily = denomFamily;
        }

        @Override public String toString() {
            return value + " " + numerUnit + "/" + denomUnit;
        }
    }

    // ── Expression nodes (§4.3 <expression>) ─────────────────────────────────

    public interface Expr {}

    public static final class QuantityExpr implements Expr {
        public final Quantity qty;
        public QuantityExpr(Quantity qty) { this.qty = qty; }
    }

    public static final class NumExpr implements Expr {
        public final double value;
        public NumExpr(double value) { this.value = value; }
    }

    public static final class IdentExpr implements Expr {
        public final String name;
        public IdentExpr(String name) { this.name = name; }
    }

    public static final class BinaryExpr implements Expr {
        public final Expr   left;
        public final String op;   // "+" or "-"
        public final Expr   right;

        public BinaryExpr(Expr left, String op, Expr right) {
            this.left  = left;
            this.op    = op;
            this.right = right;
        }
    }

    // ── Declaration nodes ────────────────────────────────────────────────────

    public static final class ExerciseDecl {
        public final String   label;
        public final int      sets;
        public final int      reps;
        public final Quantity weight;

        public ExerciseDecl(String label, int sets, int reps, Quantity weight) {
            this.label  = label;
            this.sets   = sets;
            this.reps   = reps;
            this.weight = weight;
        }
    }

    public static final class ProgressStmt {
        public final Rate rate;
        public ProgressStmt(Rate rate) { this.rate = rate; }
    }

    public static final class WorkoutDecl {
        public final String            label;
        public final List<ExerciseDecl> exercises;
        public final ProgressStmt      progress; // null if absent

        public WorkoutDecl(String label, List<ExerciseDecl> exercises, ProgressStmt progress) {
            this.label     = label;
            this.exercises = exercises;
            this.progress  = progress;
        }
    }

    public static final class MacroDecl {
        public final MacroName name;
        public final Quantity  quantity;

        public MacroDecl(MacroName name, Quantity quantity) {
            this.name     = name;
            this.quantity = quantity;
        }
    }

    public static final class MealDecl {
        public final String          label;
        public final List<MacroDecl> macros;

        public MealDecl(String label, List<MacroDecl> macros) {
            this.label  = label;
            this.macros = macros;
        }
    }

    // ── Rule declarations (§4.3 <rule-decl>) ─────────────────────────────────

    public interface RuleDecl {}

    public static final class TargetDecl implements RuleDecl {
        public final MacroName  macro;
        public final String     relOp;
        public final Quantity   qty;
        public final String     perUnit;    // null if no "per X of bodyweight"
        public final UnitFamily perFamily;  // null iff perUnit is null

        public TargetDecl(MacroName macro, String relOp, Quantity qty,
                          String perUnit, UnitFamily perFamily) {
            this.macro     = macro;
            this.relOp     = relOp;
            this.qty       = qty;
            this.perUnit   = perUnit;
            this.perFamily = perFamily;
        }
    }

    public static final class GoalRateDecl implements RuleDecl {
        public final Direction direction;
        public final Rate      rate;

        public GoalRateDecl(Direction direction, Rate rate) {
            this.direction = direction;
            this.rate      = rate;
        }
    }

    public static final class LetDecl implements RuleDecl {
        public final String name;
        public final Expr   expr;

        public LetDecl(String name, Expr expr) {
            this.name = name;
            this.expr = expr;
        }
    }

    // ── Schedule (§4.3 <schedule-stmt>) ──────────────────────────────────────

    public static final class ScheduleStmt {
        public final List<String> workouts; // null if workouts clause absent
        public final List<String> meals;    // null if meals clause absent

        public ScheduleStmt(List<String> workouts, List<String> meals) {
            this.workouts = workouts;
            this.meals    = meals;
        }
    }

    // ── Top-level nodes ───────────────────────────────────────────────────────

    public static final class AthleteParams {
        public final Quantity bodyweight;
        public final GoalMode goal;

        public AthleteParams(Quantity bodyweight, GoalMode goal) {
            this.bodyweight = bodyweight;
            this.goal       = goal;
        }
    }

    public static final class AthleteBlock {
        public final AthleteParams    params;
        public final List<WorkoutDecl> workouts;
        public final List<MealDecl>    meals;
        public final List<RuleDecl>    rules;
        public final ScheduleStmt     schedule;

        public AthleteBlock(AthleteParams params, List<WorkoutDecl> workouts,
                            List<MealDecl> meals, List<RuleDecl> rules,
                            ScheduleStmt schedule) {
            this.params   = params;
            this.workouts = workouts;
            this.meals    = meals;
            this.rules    = rules;
            this.schedule = schedule;
        }
    }

    private Ast() {}
}
