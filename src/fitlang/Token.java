package fitlang;

public final class Token {

    public enum Type {
        // Reserved words
        ATHLETE, WORKOUT, EXERCISE, MEAL, PROGRESS, TARGET, GOAL, PLAN, WEEK,
        WITH, WORKOUTS, MEALS, LET, BODYWEIGHT,
        SETS, REPS, WEIGHT, PROTEIN, CARBS, FAT,
        PER, OF, AND,
        // Mode literals
        BULK, CUT, MAINTAIN, LOSE, GAIN,
        // Unit tokens — mass
        G, KG, LB,
        // Unit tokens — energy
        KCAL, KJ,
        // Unit tokens — time  (WEEK already above serves as time unit too)
        S, MIN, H, DAY,
        // Value literals
        NUM_LIT, STRING_LIT, IDENT,
        // Punctuation
        LBRACE, RBRACE, LBRACKET, RBRACKET, LPAREN, RPAREN,
        COMMA, COLON, SLASH, PLUS, MINUS, STAR, ASSIGN,
        GT, LT, GTE, LTE, EQEQ, NEQ,
        // Sentinel
        EOF
    }

    public final Type   type;
    public final String lexeme;
    public final int    line;
    public final int    col;

    public Token(Type type, String lexeme, int line, int col) {
        this.type   = type;
        this.lexeme = lexeme;
        this.line   = line;
        this.col    = col;
    }

    @Override
    public String toString() {
        return type + "(\"" + lexeme + "\") at " + line + ":" + col;
    }
}
