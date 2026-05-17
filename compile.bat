@echo off
if not exist out mkdir out
javac -d out src\fitlang\FitLangException.java src\fitlang\Token.java src\fitlang\ast\Ast.java src\fitlang\Lexer.java src\fitlang\Parser.java src\fitlang\AstDumper.java src\fitlang\TypeChecker.java src\fitlang\Main.java
if %errorlevel% equ 0 (
    echo Build successful.
) else (
    echo Build failed.
    exit /b 1
)
