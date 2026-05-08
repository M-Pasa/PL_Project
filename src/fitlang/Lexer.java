package fitlang;

import java.util.*;

/**
 * FitLang scanner.  Converts source text to a token list (ending with EOF).
 * Implements §4.2 of D1: UTF-8 source, # line comments, maximal-munch
 * punctuation, closed lexicon for keywords/mode-literals/units.
 */
public final class Lexer {

    private static final Map<String, Token.Type> KEYWORDS = new HashMap<>();
    private static final Map<String, Token.Type> UNITS    = new HashMap<>();

    static {
        // Reserved words (§4.2.5)
        KEYWORDS.put("athlete",    Token.Type.ATHLETE);
        KEYWORDS.put("workout",    Token.Type.WORKOUT);
        KEYWORDS.put("exercise",   Token.Type.EXERCISE);
        KEYWORDS.put("meal",       Token.Type.MEAL);
        KEYWORDS.put("progress",   Token.Type.PROGRESS);
        KEYWORDS.put("target",     Token.Type.TARGET);
        KEYWORDS.put("goal",       Token.Type.GOAL);
        KEYWORDS.put("plan",       Token.Type.PLAN);
        KEYWORDS.put("week",       Token.Type.WEEK);   // dual: reserved word + time unit
        KEYWORDS.put("with",       Token.Type.WITH);
        KEYWORDS.put("workouts",   Token.Type.WORKOUTS);
        KEYWORDS.put("meals",      Token.Type.MEALS);
        KEYWORDS.put("let",        Token.Type.LET);
        KEYWORDS.put("bodyweight", Token.Type.BODYWEIGHT);
        KEYWORDS.put("sets",       Token.Type.SETS);
        KEYWORDS.put("reps",       Token.Type.REPS);
        KEYWORDS.put("weight",     Token.Type.WEIGHT);
        KEYWORDS.put("protein",    Token.Type.PROTEIN);
        KEYWORDS.put("carbs",      Token.Type.CARBS);
        KEYWORDS.put("fat",        Token.Type.FAT);
        KEYWORDS.put("per",        Token.Type.PER);
        KEYWORDS.put("of",         Token.Type.OF);
        KEYWORDS.put("and",        Token.Type.AND);
        // Control structure + routine template (§4.3 extension)
        KEYWORDS.put("when",       Token.Type.WHEN);
        KEYWORDS.put("else",       Token.Type.ELSE);
        KEYWORDS.put("routine",    Token.Type.ROUTINE);
        KEYWORDS.put("use",        Token.Type.USE);
        // Mode literals (§4.2.5) — in the same closed lexicon, different token class
        KEYWORDS.put("bulk",       Token.Type.BULK);
        KEYWORDS.put("cut",        Token.Type.CUT);
        KEYWORDS.put("maintain",   Token.Type.MAINTAIN);
        KEYWORDS.put("lose",       Token.Type.LOSE);
        KEYWORDS.put("gain",       Token.Type.GAIN);
        // Units (§4.2.6) — "week" handled above; kJ is case-sensitive
        UNITS.put("g",    Token.Type.G);
        UNITS.put("kg",   Token.Type.KG);
        UNITS.put("lb",   Token.Type.LB);
        UNITS.put("kcal", Token.Type.KCAL);
        UNITS.put("kJ",   Token.Type.KJ);
        UNITS.put("s",    Token.Type.S);
        UNITS.put("min",  Token.Type.MIN);
        UNITS.put("h",    Token.Type.H);
        UNITS.put("day",  Token.Type.DAY);
    }

    private final String src;
    private int pos  = 0;
    private int line = 1;
    private int col  = 1;

    public Lexer(String src) { this.src = src; }

    /** Tokenize the entire source and return the token list (includes EOF). */
    public List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (pos < src.length()) {
            skipWhitespaceAndComments();
            if (pos >= src.length()) break;
            out.add(nextToken());
        }
        out.add(new Token(Token.Type.EOF, "", line, col));
        return out;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private char cur() { return src.charAt(pos); }

    private void advance() {
        if (cur() == '\n') { line++; col = 1; } else { col++; }
        pos++;
    }

    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            char c = cur();
            if (c == '#') {
                while (pos < src.length() && cur() != '\n') advance();
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else {
                break;
            }
        }
    }

    private Token nextToken() {
        int sl = line, sc = col;
        char c = cur();
        if (c == '"')                              return scanString(sl, sc);
        if (Character.isDigit(c))                 return scanNumber(sl, sc);
        if (Character.isLetter(c) || c == '_')    return scanWord(sl, sc);
        return scanPunct(sl, sc);
    }

    private Token scanString(int sl, int sc) {
        advance(); // skip opening "
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && cur() != '"') {
            char c = cur();
            if (c == '\n')
                throw new FitLangException("Unterminated string literal", sl, sc);
            if (c == '\\') {
                advance();
                if (pos >= src.length())
                    throw new FitLangException("Unterminated escape sequence", sl, sc);
                char esc = cur();
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    default:
                        throw new FitLangException("Unknown escape '\\" + esc + "'", line, col);
                }
                advance();
            } else {
                sb.append(c);
                advance();
            }
        }
        if (pos >= src.length())
            throw new FitLangException("Unterminated string literal", sl, sc);
        advance(); // skip closing "
        return new Token(Token.Type.STRING_LIT, sb.toString(), sl, sc);
    }

    private Token scanNumber(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && Character.isDigit(cur())) {
            sb.append(cur()); advance();
        }
        if (pos < src.length() && cur() == '.'
                && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1))) {
            sb.append('.'); advance();
            while (pos < src.length() && Character.isDigit(cur())) {
                sb.append(cur()); advance();
            }
        }
        return new Token(Token.Type.NUM_LIT, sb.toString(), sl, sc);
    }

    private Token scanWord(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && (Character.isLetterOrDigit(cur()) || cur() == '_')) {
            sb.append(cur()); advance();
        }
        String word = sb.toString();
        Token.Type kw = KEYWORDS.get(word);
        if (kw != null) return new Token(kw, word, sl, sc);
        Token.Type unit = UNITS.get(word);
        if (unit != null) return new Token(unit, word, sl, sc);
        return new Token(Token.Type.IDENT, word, sl, sc);
    }

    private Token scanPunct(int sl, int sc) {
        char c = cur();
        // Maximal-munch: try two-character operators first (§4.2.7)
        if (pos + 1 < src.length()) {
            String two = "" + c + src.charAt(pos + 1);
            Token.Type tt = twoChar(two);
            if (tt != null) {
                advance(); advance();
                return new Token(tt, two, sl, sc);
            }
        }
        Token.Type tt = oneChar(c);
        if (tt == null)
            throw new FitLangException("Unexpected character '" + c + "'", sl, sc);
        advance();
        return new Token(tt, String.valueOf(c), sl, sc);
    }

    private static Token.Type twoChar(String s) {
        switch (s) {
            case ">=": return Token.Type.GTE;
            case "<=": return Token.Type.LTE;
            case "==": return Token.Type.EQEQ;
            case "!=": return Token.Type.NEQ;
            default:   return null;
        }
    }

    private static Token.Type oneChar(char c) {
        switch (c) {
            case '{': return Token.Type.LBRACE;
            case '}': return Token.Type.RBRACE;
            case '[': return Token.Type.LBRACKET;
            case ']': return Token.Type.RBRACKET;
            case '(': return Token.Type.LPAREN;
            case ')': return Token.Type.RPAREN;
            case ',': return Token.Type.COMMA;
            case ':': return Token.Type.COLON;
            case '/': return Token.Type.SLASH;
            case '+': return Token.Type.PLUS;
            case '-': return Token.Type.MINUS;
            case '*': return Token.Type.STAR;
            case '=': return Token.Type.ASSIGN;
            case '>': return Token.Type.GT;
            case '<': return Token.Type.LT;
            default:  return null;
        }
    }
}
