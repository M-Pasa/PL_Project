package fitlang;

import fitlang.ast.Ast;
import java.util.List;

/** Pretty-prints a FitLang AST as an indented text tree (--dump-ast output). */
public final class AstDumper {

    private final StringBuilder out    = new StringBuilder();
    private int                 indent = 0;

    public String dump(Ast.AthleteBlock block) {
        dumpAthleteBlock(block);
        return out.toString();
    }

    // ── indent helpers ───────────────────────────────────────────────────────

    private void line(String s) {
        for (int i = 0; i < indent; i++) out.append("  ");
        out.append(s).append('\n');
    }

    private void push() { indent++; }
    private void pop()  { indent--; }

    // ── node dumpers ─────────────────────────────────────────────────────────

    private void dumpAthleteBlock(Ast.AthleteBlock b) {
        line("AthleteBlock");
        push();
        dumpAthleteParams(b.params);
        for (Ast.RoutineDecl r : b.routines) dumpRoutineDecl(r);
        for (Ast.WorkoutDecl w : b.workouts) dumpWorkoutDecl(w);
        for (Ast.MealDecl   m : b.meals)    dumpMealDecl(m);
        if (!b.rules.isEmpty()) {
            line("RulesSection");
            push();
            for (Ast.RuleDecl r : b.rules) dumpRuleDecl(r);
            pop();
        }
        dumpScheduleStmt(b.schedule);
        pop();
    }

    private void dumpAthleteParams(Ast.AthleteParams p) {
        line("AthleteParams");
        push();
        line("bodyweight : " + p.bodyweight + "  [" + p.bodyweight.family + "]");
        line("goal       : " + p.goal.name().toLowerCase());
        pop();
    }

    private void dumpWorkoutDecl(Ast.WorkoutDecl w) {
        line("WorkoutDecl \"" + w.label + "\"");
        push();
        for (Ast.WorkoutItem it : w.items) {
            if (it instanceof Ast.ExerciseDecl) dumpExerciseDecl((Ast.ExerciseDecl) it);
            else if (it instanceof Ast.RoutineCall) dumpRoutineCall((Ast.RoutineCall) it);
        }
        if (w.progress != null)
            line("ProgressStmt rate=" + w.progress.rate);
        pop();
    }

    private void dumpRoutineDecl(Ast.RoutineDecl r) {
        StringBuilder sig = new StringBuilder();
        for (int i = 0; i < r.params.size(); i++) {
            if (i > 0) sig.append(", ");
            Ast.RoutineParam p = r.params.get(i);
            sig.append(p.name).append(":").append(p.unit)
               .append(" [").append(p.family).append("]");
        }
        line("RoutineDecl \"" + r.label + "\" (" + sig + ")");
        push();
        for (Ast.ExerciseDecl e : r.exercises) dumpExerciseDecl(e);
        if (r.progress != null)
            line("ProgressStmt rate=" + r.progress.rate);
        pop();
    }

    private void dumpRoutineCall(Ast.RoutineCall c) {
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < c.args.size(); i++) {
            if (i > 0) args.append(", ");
            Ast.Quantity q = c.args.get(i);
            args.append(q).append(" [").append(q.family).append("]");
        }
        line("RoutineCall \"" + c.label + "\" (" + args + ")");
    }

    private void dumpExerciseDecl(Ast.ExerciseDecl e) {
        line("ExerciseDecl \"" + e.label + "\"");
        push();
        line("sets   : " + e.sets);
        line("reps   : " + e.reps);
        line("weight : " + e.weight + "  [" + e.weight.family + "]");
        pop();
    }

    private void dumpMealDecl(Ast.MealDecl m) {
        line("MealDecl \"" + m.label + "\"");
        push();
        for (Ast.MacroDecl mc : m.macros)
            line("MacroDecl " + mc.name.name().toLowerCase()
                 + " : " + mc.quantity + "  [" + mc.quantity.family + "]");
        pop();
    }

    private void dumpRuleDecl(Ast.RuleDecl r) {
        if (r instanceof Ast.TargetDecl) {
            Ast.TargetDecl t = (Ast.TargetDecl) r;
            String perSuffix = t.perUnit != null
                ? " per " + t.perUnit + " of bodyweight"
                : "";
            line("TargetDecl "
                 + t.macro.name().toLowerCase() + " " + t.relOp + " " + t.qty + perSuffix);
        } else if (r instanceof Ast.GoalRateDecl) {
            Ast.GoalRateDecl g = (Ast.GoalRateDecl) r;
            line("GoalRateDecl " + g.direction.name().toLowerCase() + " " + g.rate);
        } else if (r instanceof Ast.LetDecl) {
            Ast.LetDecl ld = (Ast.LetDecl) r;
            line("LetDecl " + ld.name + " = " + exprStr(ld.expr));
        } else if (r instanceof Ast.WhenDecl) {
            Ast.WhenDecl w = (Ast.WhenDecl) r;
            line("WhenDecl goal=" + w.mode.name().toLowerCase());
            push();
            line("then");
            push();
            for (Ast.RuleDecl tr : w.thenRules) dumpRuleDecl(tr);
            pop();
            if (w.elseRules != null) {
                line("else");
                push();
                for (Ast.RuleDecl er : w.elseRules) dumpRuleDecl(er);
                pop();
            }
            pop();
        }
    }

    private String exprStr(Ast.Expr e) {
        if (e instanceof Ast.QuantityExpr) return ((Ast.QuantityExpr) e).qty.toString();
        if (e instanceof Ast.RateExpr)     return ((Ast.RateExpr) e).rate.toString();
        if (e instanceof Ast.NumExpr)      return String.valueOf(((Ast.NumExpr) e).value);
        if (e instanceof Ast.IdentExpr)    return ((Ast.IdentExpr) e).name;
        if (e instanceof Ast.UnaryExpr) {
            Ast.UnaryExpr u = (Ast.UnaryExpr) e;
            return "(" + u.op + exprStr(u.operand) + ")";
        }
        if (e instanceof Ast.BinaryExpr) {
            Ast.BinaryExpr b = (Ast.BinaryExpr) e;
            return "(" + exprStr(b.left) + " " + b.op + " " + exprStr(b.right) + ")";
        }
        return "?";
    }

    private void dumpScheduleStmt(Ast.ScheduleStmt s) {
        line("ScheduleStmt");
        push();
        if (s.workouts != null) line("workouts : " + fmtList(s.workouts));
        if (s.meals    != null) line("meals    : " + fmtList(s.meals));
        pop();
    }

    private String fmtList(List<String> names) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(names.get(i)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
