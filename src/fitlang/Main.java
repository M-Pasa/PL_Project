package fitlang;

import fitlang.ast.Ast;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * FitLang entry point.
 *
 * Usage:
 *   fitlang <file.fl>             -- parse and report OK or error
 *   fitlang <file.fl> --dump-ast  -- parse and print the AST
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: fitlang <file.fl> [--dump-ast]");
            System.exit(1);
        }

        String  filePath = null;
        boolean dumpAst  = false;

        for (String arg : args) {
            if (arg.equals("--dump-ast")) dumpAst = true;
            else                          filePath = arg;
        }

        if (filePath == null) {
            System.err.println("Error: no input file specified.");
            System.exit(1);
        }

        String source;
        try {
            source = Files.readString(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("Error reading '" + filePath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            List<Token>      tokens  = new Lexer(source).tokenize();
            Ast.AthleteBlock program = new Parser(tokens).parse();

            if (dumpAst) {
                System.out.print(new AstDumper().dump(program));
            } else {
                System.out.println("OK: '" + filePath + "' parsed successfully.");
            }

        } catch (FitLangException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.exit(1);
        }
    }
}
