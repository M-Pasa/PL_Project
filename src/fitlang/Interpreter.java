package fitlang;

import fitlang.ast.Ast;
import fitlang.ast.Ast.UnitFamily;
import java.util.*;

/**
 * FitLang tree-walking interpreter (§4.4).
 *
 * Assumes a well-typed AST: type errors are the type checker's job. The
 * interpreter never re-validates families or operator algebra; it only
 * computes. The single runtime fault that can survive type-checking is
 * division by zero in a Number/Number expression — reported via
 * {@link FitLangException}.
 *
 * Values are canonicalized at construction (§4.4.3): Mass → grams, Energy →
 * kcal, Time → seconds. Display units are chosen per role at print time,
 * not per family — bodyweight prints in kg, macros in g, etc. (§4.4 print
 * policy decided 2026-05-18; see D4 entry).
 *
 * Multi-week projection (§4.4.8): the schedule carries an N from
 * `plan [N] week …` (default N=1). For each week k ∈ 1..N the interpreter
 * projects bodyweight as bw₀ ± goalRate × ((k-1) week) and applies any
 * workout's `progress weight r` per-exercise as w₀ + r × ((k-1) week). Meals
 * are constant across weeks (§4.4.8) and targets recompute against each
 * week's projected bodyweight.
 */
public final class Interpreter {

    // ── Values ───────────────────────────────────────────────────────────────

    public static abstract class Value {}

    public static final class NumV extends Value {
        public final double v;
        public NumV(double v) { this.v = v; }
    }

    /** Mass in canonical unit (grams). */
    public static final class MassV extends Value {
        public final double g;
        public MassV(double g) { this.g = g; }
    }

    /** Energy in canonical unit (kcal). */
    public static final class EnergyV extends Value {
        public final double kcal;
        public EnergyV(double kcal) { this.kcal = kcal; }
    }

    /** Time in canonical unit (seconds). */
    public static final class TimeV extends Value {
        public final double s;
        public TimeV(double s) { this.s = s; }
    }

    /** Rate in canonical/canonical (e.g. g/s for Mass/Time). */
    public static final class RateV extends Value {
        public final double     v;
        public final UnitFamily numer;
        public final UnitFamily denom;
        public RateV(double v, UnitFamily n, UnitFamily d) {
            this.v = v; this.numer = n; this.denom = d;
        }
    }

    // ── Canonicalization factors (multiply source value by factor) ───────────

    private static double toCanonical(double value, String unit) {
        switch (unit) {
            // Mass → g
            case "g":    return value;
            case "kg":   return value * 1000.0;
            case "lb":   return value * 453.59237;
            // Energy → kcal
            case "kcal": return value;
            case "kJ":   return value * 0.2390057;
            // Time → s
            case "s":    return value;
            case "min":  return value * 60.0;
            case "h":    return value * 3600.0;
            case "day":  return value * 86400.0;
            case "week": return value * 604800.0;
            default:
                throw new IllegalStateException("Unknown unit: " + unit);
        }
    }

    private static Value liftQuantity(Ast.Quantity q) {
        double c = toCanonical(q.value, q.unit);
        switch (q.family) {
            case MASS:   return new MassV(c);
            case ENERGY: return new EnergyV(c);
            case TIME:   return new TimeV(c);
            default: throw new IllegalStateException("Bad family");
        }
    }

    private static RateV liftRate(Ast.Rate r) {
        double num = toCanonical(r.value, r.numerUnit);  // numerator canonicalized
        double den = toCanonical(1.0,     r.denomUnit);  // 1 of denom unit in canonical
        return new RateV(num / den, r.numerFamily, r.denomFamily);
    }

    // ── Display helpers (role-keyed; deltas use measured-side role) ──────────

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.format("%d", (long) d);
        return String.format("%.3f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Mass display for the bodyweight / exercise-weight role: print in kg. */
    private static String massAsKg(MassV m) { return fmt(m.g / 1000.0) + " kg"; }

    /** Mass display for the macro role: print in g. */
    private static String massAsG(MassV m) { return fmt(m.g) + " g"; }

    /** Time display for the horizon role: print in weeks. */
    private static String timeAsWeek(TimeV t) { return fmt(t.s / 604800.0) + " week"; }

    /** Rate(Mass,Time) display: kg/week. */
    private static String rateAsKgPerWeek(RateV r) {
        return fmt(r.v * 604800.0 / 1000.0) + " kg/week";
    }

    /** Echo a quantity in its declared source unit (for target thresholds). */
    private static String echoQuantity(Ast.Quantity q) {
        return fmt(q.value) + " " + q.unit;
    }

    // ── Environment ──────────────────────────────────────────────────────────

    private final Map<String, Value> env = new LinkedHashMap<>();
    private final Map<String, Ast.RoutineDecl> routines = new LinkedHashMap<>();
    private final Map<String, Ast.WorkoutDecl> workouts = new LinkedHashMap<>();
    private final Map<String, Ast.MealDecl>    meals    = new LinkedHashMap<>();

    // ── Entry ────────────────────────────────────────────────────────────────

    public Plan run(Ast.AthleteBlock b) {
        for (Ast.RoutineDecl r : b.routines) routines.put(r.label, r);
        for (Ast.WorkoutDecl w : b.workouts) workouts.put(w.label, w);
        for (Ast.MealDecl    m : b.meals)    meals.put(m.label, m);

        // §4.4.5: evaluate let-decls into the env; static when-dispatch (§4.4.10)
        // splices the surviving branch into the rule stream.
        List<Ast.RuleDecl> liveRules = new ArrayList<>();
        spliceRules(b.rules, b.params.goal, liveRules);
        for (Ast.RuleDecl r : liveRules) {
            if (r instanceof Ast.LetDecl) {
                Ast.LetDecl ld = (Ast.LetDecl) r;
                env.put(ld.name, evalExpr(ld.expr));
            }
        }

        int N = b.schedule.weeks;
        Plan plan = new Plan();
        plan.athleteBodyweight = (MassV) liftQuantity(b.params.bodyweight);
        plan.athleteGoal       = b.params.goal;
        plan.horizon           = new TimeV(toCanonical(N, "week"));
        plan.goalRate          = findGoalRate(liveRules);

        // §4.4.8: signed kg/s change per second of elapsed time. lose negates.
        double bwChangePerSec = 0.0;
        if (plan.goalRate != null) {
            int sign = (plan.goalRate.direction == Ast.Direction.LOSE) ? -1 : +1;
            bwChangePerSec = sign * plan.goalRate.rate.v; // g/s already canonical
        }
        double weekInSec = toCanonical(1.0, "week");

        for (int k = 1; k <= N; k++) {
            double offsetSec = (k - 1) * weekInSec;
            Plan.Week week = new Plan.Week();
            week.index      = k;
            week.bodyweight = new MassV(plan.athleteBodyweight.g + bwChangePerSec * offsetSec);

            if (b.schedule.workouts != null) {
                for (String wl : b.schedule.workouts) {
                    week.workouts.add(evalWorkout(workouts.get(wl), offsetSec));
                }
            }
            if (b.schedule.meals != null) {
                for (String ml : b.schedule.meals) {
                    week.meals.add(evalMeal(meals.get(ml)));
                }
            }
            week.macroTotals = sumMacros(week.meals);

            for (Ast.RuleDecl r : liveRules) {
                if (r instanceof Ast.TargetDecl) {
                    week.targets.add(evalTarget(
                        (Ast.TargetDecl) r, week.macroTotals, week.bodyweight));
                }
            }
            plan.weeks.add(week);
        }
        return plan;
    }

    // ── Rule splicing ────────────────────────────────────────────────────────

    private void spliceRules(List<Ast.RuleDecl> in, Ast.GoalMode goal,
                             List<Ast.RuleDecl> out) {
        for (Ast.RuleDecl r : in) {
            if (r instanceof Ast.WhenDecl) {
                Ast.WhenDecl w = (Ast.WhenDecl) r;
                List<Ast.RuleDecl> surviving =
                    (w.mode == goal) ? w.thenRules : w.elseRules;
                if (surviving != null) spliceRules(surviving, goal, out);
            } else {
                out.add(r);
            }
        }
    }

    private Plan.GoalRate findGoalRate(List<Ast.RuleDecl> rules) {
        for (Ast.RuleDecl r : rules) {
            if (r instanceof Ast.GoalRateDecl) {
                Ast.GoalRateDecl g = (Ast.GoalRateDecl) r;
                Plan.GoalRate out = new Plan.GoalRate();
                out.direction = g.direction;
                out.rate      = liftRate(g.rate);
                return out;
            }
        }
        return null;
    }

    // ── Workouts ─────────────────────────────────────────────────────────────

    private Plan.WorkoutResult evalWorkout(Ast.WorkoutDecl w, double offsetSec) {
        Plan.WorkoutResult out = new Plan.WorkoutResult();
        out.label = w.label;
        double wkPerSec = (w.progress == null) ? 0.0 : liftRate(w.progress.rate).v;
        for (Ast.WorkoutItem it : w.items) {
            if (it instanceof Ast.ExerciseDecl) {
                out.exercises.add(evalExercise((Ast.ExerciseDecl) it, wkPerSec, offsetSec));
            } else if (it instanceof Ast.RoutineCall) {
                Ast.RoutineCall rc = (Ast.RoutineCall) it;
                Ast.RoutineDecl rd = routines.get(rc.label);
                double rkPerSec = (rd.progress == null) ? 0.0 : liftRate(rd.progress.rate).v;
                // §4.4.7: bind params, evaluate routine body in extended env.
                // (Args are <quantity> in P1; no real expressions yet.)
                Map<String, Value> savedEnv = new LinkedHashMap<>(env);
                for (int i = 0; i < rd.params.size(); i++) {
                    env.put(rd.params.get(i).name, liftQuantity(rc.args.get(i)));
                }
                for (Ast.ExerciseDecl e : rd.exercises) {
                    out.exercises.add(evalExercise(e, rkPerSec, offsetSec));
                }
                env.clear(); env.putAll(savedEnv); // restore — no closure leak
            }
        }
        return out;
    }

    private Plan.ExerciseResult evalExercise(Ast.ExerciseDecl e,
                                             double progressGPerSec, double offsetSec) {
        Plan.ExerciseResult r = new Plan.ExerciseResult();
        r.label  = e.label;
        r.sets   = e.sets;
        r.reps   = e.reps;
        double base_g = ((MassV) liftQuantity(e.weight)).g;
        r.weight = new MassV(base_g + progressGPerSec * offsetSec);
        return r;
    }

    // ── Meals & macros ───────────────────────────────────────────────────────

    private Plan.MealResult evalMeal(Ast.MealDecl m) {
        Plan.MealResult out = new Plan.MealResult();
        out.label = m.label;
        for (Ast.MacroDecl md : m.macros) {
            out.macros.put(md.name, (MassV) liftQuantity(md.quantity));
        }
        return out;
    }

    private EnumMap<Ast.MacroName, MassV> sumMacros(List<Plan.MealResult> meals) {
        EnumMap<Ast.MacroName, MassV> total = new EnumMap<>(Ast.MacroName.class);
        for (Plan.MealResult m : meals) {
            for (Map.Entry<Ast.MacroName, MassV> e : m.macros.entrySet()) {
                MassV cur = total.get(e.getKey());
                double sum = (cur == null ? 0.0 : cur.g) + e.getValue().g;
                total.put(e.getKey(), new MassV(sum));
            }
        }
        return total;
    }

    // ── Targets (§4.4.9) ─────────────────────────────────────────────────────

    private Plan.TargetResult evalTarget(Ast.TargetDecl t,
                                         EnumMap<Ast.MacroName, MassV> macros,
                                         MassV bodyweight) {
        Plan.TargetResult r = new Plan.TargetResult();
        r.macro = t.macro;
        r.relOp = t.relOp;
        r.thresholdEcho = echoQuantity(t.qty);

        double threshold_g = toCanonical(t.qty.value, t.qty.unit);
        if (t.perUnit != null) {
            // "qty per perUnit of bodyweight" → multiply by bodyweight / 1-of-perUnit
            double perUnitCanonical = toCanonical(1.0, t.perUnit);
            threshold_g = threshold_g * (bodyweight.g / perUnitCanonical);
            r.thresholdEcho += " per " + t.perUnit + " of bodyweight ("
                + fmt(threshold_g) + " g)";
        }

        MassV planned = macros.get(t.macro);
        double planned_g = (planned == null ? 0.0 : planned.g);
        r.plannedEcho = fmt(planned_g) + " g";

        boolean ok;
        switch (t.relOp) {
            case ">=": ok = planned_g >= threshold_g; break;
            case "<=": ok = planned_g <= threshold_g; break;
            case "==": ok = planned_g == threshold_g; break;
            default:   ok = false;
        }
        r.ok = ok;
        double deltaG = planned_g - threshold_g;
        r.deltaEcho = (deltaG >= 0 ? "+" : "") + fmt(deltaG) + " g";
        return r;
    }

    // ── Expressions (§4.4.4) ─────────────────────────────────────────────────

    private Value evalExpr(Ast.Expr e) {
        if (e instanceof Ast.NumExpr)      return new NumV(((Ast.NumExpr) e).value);
        if (e instanceof Ast.QuantityExpr) return liftQuantity(((Ast.QuantityExpr) e).qty);
        if (e instanceof Ast.RateExpr)     return liftRate(((Ast.RateExpr) e).rate);
        if (e instanceof Ast.IdentExpr) {
            Value v = env.get(((Ast.IdentExpr) e).name);
            if (v == null) throw new IllegalStateException(
                "interpreter: unbound identifier '" + ((Ast.IdentExpr) e).name
                + "' — type checker should have caught this");
            return v;
        }
        if (e instanceof Ast.UnaryExpr) {
            Ast.UnaryExpr u = (Ast.UnaryExpr) e;
            Value inner = evalExpr(u.operand);
            return negate(inner);
        }
        if (e instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr b = (Ast.BinaryExpr) e;
            return applyBinary(b.op, evalExpr(b.left), evalExpr(b.right));
        }
        throw new IllegalStateException("interpreter: unhandled expression node "
            + e.getClass().getSimpleName());
    }

    private Value negate(Value v) {
        if (v instanceof NumV)    return new NumV(-((NumV) v).v);
        if (v instanceof MassV)   return new MassV(-((MassV) v).g);
        if (v instanceof EnergyV) return new EnergyV(-((EnergyV) v).kcal);
        if (v instanceof TimeV)   return new TimeV(-((TimeV) v).s);
        if (v instanceof RateV) {
            RateV r = (RateV) v;
            return new RateV(-r.v, r.numer, r.denom);
        }
        throw new IllegalStateException("negate: unhandled value " + v);
    }

    /**
     * Operator dispatch keyed on (op, left-class, right-class). The type checker
     * has already proved that exactly one of these cases will fire; an unhandled
     * combination would be a checker bug, not user input.
     */
    private Value applyBinary(String op, Value l, Value r) {
        switch (op) {
            case "+": return addSub(l, r, +1);
            case "-": return addSub(l, r, -1);
            case "*": return mul(l, r);
            case "/": return div(l, r);
            default:  throw new IllegalStateException("Unknown operator " + op);
        }
    }

    private Value addSub(Value l, Value r, int sign) {
        if (l instanceof NumV    && r instanceof NumV)
            return new NumV(((NumV) l).v + sign * ((NumV) r).v);
        if (l instanceof MassV   && r instanceof MassV)
            return new MassV(((MassV) l).g + sign * ((MassV) r).g);
        if (l instanceof EnergyV && r instanceof EnergyV)
            return new EnergyV(((EnergyV) l).kcal + sign * ((EnergyV) r).kcal);
        if (l instanceof TimeV   && r instanceof TimeV)
            return new TimeV(((TimeV) l).s + sign * ((TimeV) r).s);
        throw new IllegalStateException("addSub: type checker missed " + l + " " + r);
    }

    private Value mul(Value l, Value r) {
        if (l instanceof NumV && r instanceof NumV)
            return new NumV(((NumV) l).v * ((NumV) r).v);
        // Number × Quantity (and symmetric)
        if (l instanceof NumV && r instanceof MassV)   return new MassV(((NumV) l).v * ((MassV) r).g);
        if (l instanceof MassV && r instanceof NumV)   return new MassV(((MassV) l).g * ((NumV) r).v);
        if (l instanceof NumV && r instanceof EnergyV) return new EnergyV(((NumV) l).v * ((EnergyV) r).kcal);
        if (l instanceof EnergyV && r instanceof NumV) return new EnergyV(((EnergyV) l).kcal * ((NumV) r).v);
        if (l instanceof NumV && r instanceof TimeV)   return new TimeV(((NumV) l).v * ((TimeV) r).s);
        if (l instanceof TimeV && r instanceof NumV)   return new TimeV(((TimeV) l).s * ((NumV) r).v);
        // Rate × Time → Quantity(numer)  (§4.5.5)
        if (l instanceof RateV && r instanceof TimeV) {
            RateV rt = (RateV) l;
            return liftFromFamily(rt.numer, rt.v * ((TimeV) r).s);
        }
        if (l instanceof TimeV && r instanceof RateV) {
            RateV rt = (RateV) r;
            return liftFromFamily(rt.numer, rt.v * ((TimeV) l).s);
        }
        throw new IllegalStateException("mul: type checker missed " + l + " " + r);
    }

    private Value div(Value l, Value r) {
        if (l instanceof NumV && r instanceof NumV) {
            double d = ((NumV) r).v;
            if (d == 0.0) throw new FitLangException("division by zero", 0, 0);
            return new NumV(((NumV) l).v / d);
        }
        if (l instanceof MassV && r instanceof NumV) {
            double d = ((NumV) r).v;
            if (d == 0.0) throw new FitLangException("division by zero", 0, 0);
            return new MassV(((MassV) l).g / d);
        }
        if (l instanceof EnergyV && r instanceof NumV) {
            double d = ((NumV) r).v;
            if (d == 0.0) throw new FitLangException("division by zero", 0, 0);
            return new EnergyV(((EnergyV) l).kcal / d);
        }
        if (l instanceof TimeV && r instanceof NumV) {
            double d = ((NumV) r).v;
            if (d == 0.0) throw new FitLangException("division by zero", 0, 0);
            return new TimeV(((TimeV) l).s / d);
        }
        throw new IllegalStateException("div: type checker missed " + l + " " + r);
    }

    private static Value liftFromFamily(UnitFamily f, double canonical) {
        switch (f) {
            case MASS:   return new MassV(canonical);
            case ENERGY: return new EnergyV(canonical);
            case TIME:   return new TimeV(canonical);
            default: throw new IllegalStateException("liftFromFamily: " + f);
        }
    }

    // ── Plan (output structure) ──────────────────────────────────────────────

    public static final class Plan {
        public MassV          athleteBodyweight;
        public Ast.GoalMode   athleteGoal;
        public TimeV          horizon;
        public GoalRate       goalRate;        // may be null
        public final List<Week> weeks = new ArrayList<>();

        public static final class GoalRate {
            public Ast.Direction direction;
            public RateV         rate;
        }

        public static final class Week {
            public int                                 index;
            public MassV                               bodyweight;
            public final List<WorkoutResult>           workouts    = new ArrayList<>();
            public final List<MealResult>              meals       = new ArrayList<>();
            public EnumMap<Ast.MacroName, MassV>       macroTotals = new EnumMap<>(Ast.MacroName.class);
            public final List<TargetResult>            targets     = new ArrayList<>();
        }

        public static final class WorkoutResult {
            public String                  label;
            public final List<ExerciseResult> exercises = new ArrayList<>();
        }

        public static final class ExerciseResult {
            public String label;
            public int    sets;
            public int    reps;
            public MassV  weight;
        }

        public static final class MealResult {
            public String                                 label;
            public final EnumMap<Ast.MacroName, MassV>    macros = new EnumMap<>(Ast.MacroName.class);
        }

        public static final class TargetResult {
            public Ast.MacroName macro;
            public String        relOp;
            public String        thresholdEcho;
            public String        plannedEcho;
            public String        deltaEcho;
            public boolean       ok;
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FitLang Plan\n");
            sb.append("  Athlete: bodyweight = ").append(massAsKg(athleteBodyweight))
              .append(", goal = ").append(athleteGoal.name().toLowerCase()).append("\n");
            sb.append("  Horizon: ").append(timeAsWeek(horizon)).append("\n");
            if (goalRate != null) {
                sb.append("  Goal rate: ").append(goalRate.direction.name().toLowerCase())
                  .append(" at ").append(rateAsKgPerWeek(goalRate.rate)).append("\n");
            }
            for (Week w : weeks) {
                sb.append("  Week ").append(w.index)
                  .append(" (bodyweight = ").append(massAsKg(w.bodyweight)).append("):\n");
                if (!w.workouts.isEmpty()) {
                    sb.append("    Workouts:\n");
                    for (WorkoutResult wr : w.workouts) {
                        sb.append("      ").append(wr.label).append(":\n");
                        for (ExerciseResult er : wr.exercises) {
                            sb.append("        ").append(er.label)
                              .append(": ").append(er.sets).append(" × ").append(er.reps)
                              .append(" @ ").append(massAsKg(er.weight)).append("\n");
                        }
                    }
                }
                if (!w.meals.isEmpty()) {
                    sb.append("    Meals:\n");
                    for (MealResult mr : w.meals) {
                        sb.append("      ").append(mr.label).append(":");
                        for (Map.Entry<Ast.MacroName, MassV> e : mr.macros.entrySet()) {
                            sb.append(" ").append(e.getKey().name().toLowerCase())
                              .append(" ").append(massAsG(e.getValue()));
                        }
                        sb.append("\n");
                    }
                    sb.append("    Macros (total):");
                    for (Map.Entry<Ast.MacroName, MassV> e : w.macroTotals.entrySet()) {
                        sb.append(" ").append(e.getKey().name().toLowerCase())
                          .append(" ").append(massAsG(e.getValue()));
                    }
                    sb.append("\n");
                }
                if (!w.targets.isEmpty()) {
                    sb.append("    Targets:\n");
                    for (TargetResult tr : w.targets) {
                        sb.append("      ").append(tr.macro.name().toLowerCase())
                          .append(" ").append(tr.relOp)
                          .append(" ").append(tr.thresholdEcho)
                          .append(": planned ").append(tr.plannedEcho)
                          .append(" → ").append(tr.ok ? "OK" : "OFF")
                          .append(" (").append(tr.deltaEcho).append(")\n");
                    }
                }
            }
            return sb.toString();
        }
    }
}
