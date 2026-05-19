package fitlang;

import fitlang.ast.Ast;
import fitlang.ast.Ast.UnitFamily;
import java.util.*;

/**
 * FitLang static type checker (§4.5).
 *
 * Single bottom-up pass over the AST. Collects diagnostics rather than throwing;
 * the caller decides whether to stop the pipeline. The checker is intentionally
 * straight-line — no scoping stack and no inference beyond the trivial syntactic
 * dispatch the spec promises (NUM_LIT → Number, NUM_LIT UNIT → Quantity family,
 * NUM_LIT UNIT "/" UNIT → Rate(family,family)).
 *
 * What is checked, per §4.5:
 *   1. Athlete bodyweight is a Mass.
 *   2. Every exercise weight slot is a Mass.
 *   3. Every macro quantity (meal protein/carbs/fat) is a Mass.
 *   4. Every progress rate is Rate(Mass, Time).
 *   5. Every goal-rate is Rate(Mass, Time) and direction matches goal mode
 *      (bulk+lose, cut+gain are rejected — §4.4.9).
 *   6. Every target quantity is a Mass; "per UNIT of bodyweight" UNIT is Mass.
 *   7. Routine call arity and per-arg families match the declared routine.
 *   8. Schedule labels resolve to declared workouts/meals.
 *   9. Let-binding RHS expressions have type Number, Quantity D, or Rate(D₁,D₂).
 *  10. No shadowing in the let-environment (§4.6.3).
 *  11. The full arithmetic algebra of §4.5.4–§4.5.5 applies inside expressions.
 *
 * When-decl branches are dispatched **statically** (§4.4.10): the checker
 * picks the surviving branch using the athlete header `goal`, type-checks only
 * that branch, and lets its bindings leak into the rules that follow. This
 * matches the spec's "splice the surviving rule list into the enclosing
 * <rules-section>" wording. The dead branch is never checked — a type error
 * inside it is not a compile error.
 */
public final class TypeChecker {

    // ── Types ────────────────────────────────────────────────────────────────

    public static abstract class FType {
        @Override public abstract String toString();
    }
    public static final class TNumber extends FType {
        public static final TNumber INSTANCE = new TNumber();
        private TNumber() {}
        @Override public String toString() { return "Number"; }
        @Override public boolean equals(Object o) { return o instanceof TNumber; }
        @Override public int hashCode() { return 0; }
    }
    public static final class TQuantity extends FType {
        public final UnitFamily fam;
        public TQuantity(UnitFamily fam) { this.fam = fam; }
        @Override public String toString() { return "Quantity(" + fam + ")"; }
        @Override public boolean equals(Object o) {
            return o instanceof TQuantity && ((TQuantity) o).fam == fam;
        }
        @Override public int hashCode() { return fam.hashCode(); }
    }
    public static final class TRate extends FType {
        public final UnitFamily numer, denom;
        public TRate(UnitFamily n, UnitFamily d) { this.numer = n; this.denom = d; }
        @Override public String toString() { return "Rate(" + numer + "," + denom + ")"; }
        @Override public boolean equals(Object o) {
            return o instanceof TRate
                && ((TRate) o).numer == numer
                && ((TRate) o).denom == denom;
        }
        @Override public int hashCode() { return numer.hashCode() * 31 + denom.hashCode(); }
    }

    // ── State ────────────────────────────────────────────────────────────────

    private final List<String>            errors   = new ArrayList<>();
    private final Map<String, FType>      env      = new LinkedHashMap<>();
    private final Map<String, Ast.RoutineDecl> routines = new LinkedHashMap<>();
    private final Set<String>             workoutLabels = new LinkedHashSet<>();
    private final Set<String>             mealLabels    = new LinkedHashSet<>();

    public List<String> errors() { return errors; }

    private void err(String msg) { errors.add(msg); }

    // ── Entry ────────────────────────────────────────────────────────────────

    public List<String> check(Ast.AthleteBlock b) {
        if (b.params.bodyweight.family != UnitFamily.MASS)
            err("athlete bodyweight must be a Mass quantity, got "
                + b.params.bodyweight + " [" + b.params.bodyweight.family + "]");

        for (Ast.RoutineDecl r : b.routines) {
            if (routines.containsKey(r.label))
                err("duplicate routine label \"" + r.label + "\"");
            routines.put(r.label, r);
            checkRoutineDecl(r);
        }

        // §4.7 prefix let-section — bindings visible in workout/meal bodies and rules.
        for (Ast.LetDecl ld : b.lets) checkLetDecl(ld);

        for (Ast.WorkoutDecl w : b.workouts) {
            if (workoutLabels.contains(w.label))
                err("duplicate workout label \"" + w.label + "\"");
            workoutLabels.add(w.label);
            checkWorkoutDecl(w);
        }

        for (Ast.MealDecl m : b.meals) {
            if (mealLabels.contains(m.label))
                err("duplicate meal label \"" + m.label + "\"");
            mealLabels.add(m.label);
            checkMealDecl(m);
        }

        // Track whether a goal-rate has been declared (for direction/mode consistency)
        for (Ast.RuleDecl r : b.rules) checkRuleDecl(r, b.params.goal);

        checkSchedule(b.schedule);
        return errors;
    }

    // ── Declarations ─────────────────────────────────────────────────────────

    private void checkRoutineDecl(Ast.RoutineDecl r) {
        // Bind each routine parameter as Quantity(family) in the env so the body
        // can reference it. Call-site argument family-match is still enforced in
        // checkRoutineCall — the param's type here is the *declared* family.
        Map<String, FType> saved = new LinkedHashMap<>(env);
        for (Ast.RoutineParam p : r.params) env.put(p.name, new TQuantity(p.family));
        for (Ast.ExerciseDecl e : r.exercises) checkExerciseDecl(e, "routine \"" + r.label + "\"");
        if (r.progress != null) checkProgressRate(r.progress.rate, "routine \"" + r.label + "\"");
        env.clear(); env.putAll(saved);
    }

    private void checkWorkoutDecl(Ast.WorkoutDecl w) {
        for (Ast.WorkoutItem it : w.items) {
            if (it instanceof Ast.ExerciseDecl)
                checkExerciseDecl((Ast.ExerciseDecl) it, "workout \"" + w.label + "\"");
            else if (it instanceof Ast.RoutineCall)
                checkRoutineCall((Ast.RoutineCall) it);
        }
        if (w.progress != null) checkProgressRate(w.progress.rate, "workout \"" + w.label + "\"");
    }

    private void checkExerciseDecl(Ast.ExerciseDecl e, String where) {
        String ctx = where + " exercise \"" + e.label + "\" weight";
        FType t = checkExpr(e.weight, ctx);
        if (t == null) return;
        if (!(t instanceof TQuantity) || ((TQuantity) t).fam != UnitFamily.MASS)
            err(ctx + ": must be Quantity(MASS), got " + t);
    }

    private void checkProgressRate(Ast.Expr rateExpr, String where) {
        String ctx = where + " progress rate";
        FType t = checkExpr(rateExpr, ctx);
        if (t == null) return;
        if (!(t instanceof TRate)
            || ((TRate) t).numer != UnitFamily.MASS
            || ((TRate) t).denom != UnitFamily.TIME)
            err(ctx + ": must be Rate(MASS, TIME), got " + t);
    }

    private void checkRoutineCall(Ast.RoutineCall c) {
        Ast.RoutineDecl decl = routines.get(c.label);
        if (decl == null) {
            err("routine call \"" + c.label + "\" has no matching routine declaration");
            return;
        }
        if (c.args.size() != decl.params.size()) {
            err("routine call \"" + c.label + "\": expected " + decl.params.size()
                + " argument(s), got " + c.args.size());
            return;
        }
        for (int i = 0; i < c.args.size(); i++) {
            String ctx = "routine call \"" + c.label + "\" argument " + (i + 1)
                + " (" + decl.params.get(i).name + ")";
            FType t = checkExpr(c.args.get(i), ctx);
            if (t == null) continue;
            UnitFamily wanted = decl.params.get(i).family;
            if (!(t instanceof TQuantity) || ((TQuantity) t).fam != wanted)
                err(ctx + ": expected Quantity(" + wanted + "), got " + t);
        }
    }

    private void checkMealDecl(Ast.MealDecl m) {
        for (Ast.MacroDecl mc : m.macros) {
            String ctx = "meal \"" + m.label + "\" macro " + mc.name.name().toLowerCase();
            FType t = checkExpr(mc.value, ctx);
            if (t == null) continue;
            if (!(t instanceof TQuantity) || ((TQuantity) t).fam != UnitFamily.MASS)
                err(ctx + ": must be Quantity(MASS), got " + t);
        }
    }

    private void checkRuleDecl(Ast.RuleDecl r, Ast.GoalMode goalMode) {
        if      (r instanceof Ast.TargetDecl)   checkTargetDecl((Ast.TargetDecl) r);
        else if (r instanceof Ast.GoalRateDecl) checkGoalRateDecl((Ast.GoalRateDecl) r, goalMode);
        else if (r instanceof Ast.WhenDecl)     checkWhenDecl((Ast.WhenDecl) r, goalMode);
    }

    private void checkLetDecl(Ast.LetDecl ld) {
        if (env.containsKey(ld.name)) {
            err("let \"" + ld.name + "\": shadowing is not allowed (§4.6.3)");
            return;
        }
        FType t = checkExpr(ld.expr, "let " + ld.name);
        if (t == null) return; // expression already reported a sub-error
        // §4.5.7: only Number, Quantity, Rate are let-bindable. All FType subclasses
        // are admissible, so no further filter is needed beyond not-null.
        env.put(ld.name, t);
    }

    private void checkTargetDecl(Ast.TargetDecl t) {
        String ctx = "target " + t.macro.name().toLowerCase() + " threshold";
        FType ty = checkExpr(t.qty, ctx);
        if (ty != null && (!(ty instanceof TQuantity) || ((TQuantity) ty).fam != UnitFamily.MASS))
            err(ctx + ": must be Quantity(MASS), got " + ty);
        if (t.perUnit != null && t.perFamily != UnitFamily.MASS)
            err("target " + t.macro.name().toLowerCase()
                + ": 'per " + t.perUnit + " of bodyweight' unit must be a Mass unit");
    }

    private void checkGoalRateDecl(Ast.GoalRateDecl g, Ast.GoalMode goalMode) {
        String ctx = "goal " + g.direction.name().toLowerCase() + " rate";
        FType t = checkExpr(g.rate, ctx);
        if (t != null && (!(t instanceof TRate)
                || ((TRate) t).numer != UnitFamily.MASS
                || ((TRate) t).denom != UnitFamily.TIME))
            err(ctx + ": must be Rate(MASS, TIME), got " + t);
        // §4.4.9 consistency: bulk+lose and cut+gain are rejected; maintain accepts either.
        if (goalMode == Ast.GoalMode.BULK && g.direction == Ast.Direction.LOSE)
            err("goal direction 'lose' is inconsistent with athlete goal 'bulk' (§4.4.9)");
        if (goalMode == Ast.GoalMode.CUT && g.direction == Ast.Direction.GAIN)
            err("goal direction 'gain' is inconsistent with athlete goal 'cut' (§4.4.9)");
    }

    private void checkWhenDecl(Ast.WhenDecl w, Ast.GoalMode goalMode) {
        // §4.4.10: static dispatch — pick the surviving branch via the athlete
        // header `goal` and type-check only that branch. Its let-bindings then
        // extend the env for the rules that follow (the "splice" semantics).
        // The dead branch is never checked.
        List<Ast.RuleDecl> surviving =
            (w.mode == goalMode) ? w.thenRules : w.elseRules;
        if (surviving == null) return;             // dead else with no rules
        for (Ast.RuleDecl r : surviving) checkRuleDecl(r, goalMode);
    }

    private void checkSchedule(Ast.ScheduleStmt s) {
        if (s.workouts != null) for (String label : s.workouts)
            if (!workoutLabels.contains(label))
                err("schedule references unknown workout \"" + label + "\"");
        if (s.meals != null) for (String label : s.meals)
            if (!mealLabels.contains(label))
                err("schedule references unknown meal \"" + label + "\"");
    }

    // ── Expressions (§4.5.8) ─────────────────────────────────────────────────

    /** Returns null on error (and records a diagnostic); never throws. */
    private FType checkExpr(Ast.Expr e, String where) {
        if (e instanceof Ast.NumExpr)      return TNumber.INSTANCE;
        if (e instanceof Ast.QuantityExpr) return new TQuantity(((Ast.QuantityExpr) e).qty.family);
        if (e instanceof Ast.RateExpr) {
            Ast.Rate r = ((Ast.RateExpr) e).rate;
            return new TRate(r.numerFamily, r.denomFamily);
        }
        if (e instanceof Ast.IdentExpr) {
            String name = ((Ast.IdentExpr) e).name;
            FType t = env.get(name);
            if (t == null) { err(where + ": unbound identifier '" + name + "'"); return null; }
            return t;
        }
        if (e instanceof Ast.UnaryExpr) {
            FType inner = checkExpr(((Ast.UnaryExpr) e).operand, where);
            return inner; // unary "-" preserves type (§4.7.5)
        }
        if (e instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr b = (Ast.BinaryExpr) e;
            FType lt = checkExpr(b.left,  where);
            FType rt = checkExpr(b.right, where);
            if (lt == null || rt == null) return null;
            FType out = applyBinary(b.op, lt, rt);
            if (out == null)
                err(where + ": operator '" + b.op + "' is not defined on "
                    + lt + " and " + rt + " (§4.5.4–§4.5.5)");
            return out;
        }
        err(where + ": unhandled expression node " + e.getClass().getSimpleName());
        return null;
    }

    private FType applyBinary(String op, FType l, FType r) {
        switch (op) {
            case "+":
            case "-":
                // §4.5.4(1): Number+Number or Quantity D + Quantity D. Rate+Rate rejected (§4.5.5).
                if (l instanceof TNumber   && r instanceof TNumber)   return TNumber.INSTANCE;
                if (l instanceof TQuantity && r instanceof TQuantity
                    && ((TQuantity) l).fam == ((TQuantity) r).fam)    return l;
                return null;
            case "*":
                // §4.5.4(3) Number×Quantity and §4.5.5 Rate×Time.
                if (l instanceof TNumber   && r instanceof TNumber)   return TNumber.INSTANCE;
                if (l instanceof TNumber   && r instanceof TQuantity) return r;
                if (l instanceof TQuantity && r instanceof TNumber)   return l;
                if (l instanceof TRate     && r instanceof TQuantity
                    && ((TRate) l).denom == UnitFamily.TIME
                    && ((TQuantity) r).fam == UnitFamily.TIME)
                    return new TQuantity(((TRate) l).numer);
                if (l instanceof TQuantity && r instanceof TRate
                    && ((TQuantity) l).fam == UnitFamily.TIME
                    && ((TRate) r).denom  == UnitFamily.TIME)
                    return new TQuantity(((TRate) r).numer);
                return null;
            case "/":
                // §4.5.4(4): Quantity/Number or Number/Number. Same-family Q/Q rejected.
                if (l instanceof TNumber   && r instanceof TNumber)   return TNumber.INSTANCE;
                if (l instanceof TQuantity && r instanceof TNumber)   return l;
                return null;
            default:
                return null;
        }
    }
}
