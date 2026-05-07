package fitlang;

public class FitLangException extends RuntimeException {
    public final int line;
    public final int col;

    public FitLangException(String message, int line, int col) {
        super(message + " [line " + line + ", col " + col + "]");
        this.line = line;
        this.col  = col;
    }
}
