package fitlang;

public class FitLangException extends RuntimeException {
    public final int line;
    public final int col;

    public FitLangException(String message, int line, int col) {
        super(line > 0 ? message + " [line " + line + ", col " + col + "]" : message);
        this.line = line;
        this.col  = col;
    }
}
