package fitlang;

import fitlang.ast.Ast;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * FitLang entry point.
 *
 * Usage:
 *   fitlang <file.fl>             -- parse + type-check, report OK or errors
 *   fitlang <file.fl> --dump-ast  -- parse + type-check + print the AST
 *   fitlang <file.fl> --no-check  -- parse only (skip the type checker)
 */
public final class Main {

    public static void main(String[] args) {
        // Force UTF-8 on stdout/stderr so §, ⇓ etc. render correctly on Windows consoles.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length == 0) {
            System.err.println("Usage: fitlang <file.fl> [--dump-ast] [--no-check]");
            System.exit(1);
        }

        String  filePath = null;
        boolean dumpAst  = false;
        boolean noCheck  = false;

        for (String arg : args) {
            if      (arg.equals("--dump-ast")) dumpAst = true;
            else if (arg.equals("--no-check")) noCheck = true;
            else                               filePath = arg;
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

            List<String> typeErrors = List.of();
            if (!noCheck) {
                typeErrors = new TypeChecker().check(program);
            }

            if (dumpAst) {
                System.out.print(new AstDumper().dump(program));
            }

            if (!typeErrors.isEmpty()) {
                System.err.println("Type errors in '" + filePath + "':");
                for (String e : typeErrors) System.err.println("  - " + e);
                System.exit(1);
            }

            if (!dumpAst) {
                System.out.println("OK: '" + filePath
                    + "' parsed" + (noCheck ? "" : " and type-checked") + " successfully.");
            }

        } catch (FitLangException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.exit(1);
        }
    }
}
