package plc.compiler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/*
* Generator tests should be pretty simple if you have any questions message me on teams :> -Gus
* */

public class GeneratorTests {

    @Test
    void testSimplePrint(){
        Ast input = new Ast.Expression.Function("print", Arrays.asList(
                new Ast.Expression.Literal("Hello, World!")
        ));
        String expected = "print(\"Hello, World!\")";

        test(input, expected);
    }

    @Test
    void testBiggerPrint(){
        Ast input = Parser.parse(Lexer.lex("print(\"Hello, World!\");"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        print(\"Hello, World!\");" ,
                        "    }" ,
                        "" ,
                        "}"
        ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testExpression(){

        Ast input = Parser.parse(Lexer.lex("LET pi : DECIMAL = 3.14;\nLET r : INTEGER = 10;\narea = pi * (r * r);"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {",
                "" ,
                "    public static void main(String[] args) {" ,
                "        DECIMAL pi = 3.14;" ,
                "        INTEGER r = 10;" ,
                "        area = pi * (r * r);" ,
                "    }" ,
                "" ,
                "}"
                ) + System. lineSeparator();
        test(input, expected);
    }

    @Test
    void testDeclarationsmall(){
        Ast input = Parser.parse(Lexer.lex("LET str : STRING = \"hello\";\nPRINT(str);"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {",
                        "",
                        "    public static void main(String[] args) {" ,
                        "        STRING str = \"hello\";" ,
                        "        PRINT(str);" ,
                        "    }",
                        "",
                        "}"
        ) + System.lineSeparator();
        test(input, expected);
    }
    @Test
    void testDeclarationBig(){
        Ast input = Parser.parse(Lexer.lex("LET str : STRING = \"hello\";\nPRINT(str);\nLET i : INTEGER;\ni = 5;\nx = 10;\ny= \"why?\";\nPRINT(x,y);\nIF i==x THEN\n    PRINT(i);\nELSE\nEND\n"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        STRING str = \"hello\";" ,
                        "        PRINT(str);" ,
                        "        INTEGER i;" ,
                        "        i = 5;" ,
                        "        x = 10;" ,
                        "        y = \"why?\";" ,
                        "        PRINT(x, y);" ,
                        "        if (i == x) {" ,
                        "            PRINT(i);" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testIfsmall(){

        Ast input = Parser.parse(Lexer.lex("LET score : INTEGER;\nscore = score / 10;\nIF score == 5 THEN\n    PRINT(\"Gus\");\nEND"));

        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        INTEGER score;" ,
                        "        score = score / 10;" ,
                        "        if (score == 5) {" ,
                        "            PRINT(\"Gus\");" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();

        test(input, expected);

    }

    @Test
    void testIfBig(){
        Ast input = Parser.parse(Lexer.lex( "LET score : INTEGER;\nscore = score / 10;\nIF score == 10 THEN\n    " +
                "PRINT(\"A\");\nEND\nIF score == 9 THEN\n    " +
                "PRINT(\"A\");\nEND\nIF score == 8 THEN\n    " +
                "PRINT(\"B\");\nEND\nIF score == 7 THEN\n    " +
                "PRINT(\"C\");\nEND\nIF SCORE == 6 THEN\n    " +
                "PRINT(\"D\");END\nIF score != 10 THEN\n    " +
                "IF score != 9 THEN\n        " +
                "IF score != 8 THEN\n            " +
                "IF score != 7 THEN\n                " +
                "IF score != 6 THEN\n    " +
                "PRINT(\"E\");\n                " +
                "END\n            " +
                "END\n        " +
                "END\n    " +
                "END\nEND"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        INTEGER score;" ,
                        "        score = score / 10;" ,
                        "        if (score == 10) {" ,
                        "            PRINT(\"A\");" ,
                        "        }" ,
                        "        if (score == 9) {" ,
                        "            PRINT(\"A\");" ,
                        "        }" ,
                        "        if (score == 8) {" ,
                        "            PRINT(\"B\");" ,
                        "        }" ,
                        "        if (score == 7) {" ,
                        "            PRINT(\"C\");" ,
                        "        }" ,
                        "        if (SCORE == 6) {" ,
                        "            PRINT(\"D\");" ,
                        "        }" ,
                        "        if (score != 10) {" ,
                        "            if (score != 9) {" ,
                        "                if (score != 8) {" ,
                        "                    if (score != 7) {" ,
                        "                        if (score != 6) {" ,
                        "                            PRINT(\"E\");" ,
                        "                        }" ,
                        "                    }" ,
                        "                }" ,
                        "            }" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
        ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testWhileSmall(){
        Ast input = Parser.parse(Lexer.lex("LET i : INTEGER = 2;\nWHILE i != 0 DO\n    " +
                "PRINT(\"bean\");\n    i = i - 1;\nEND"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        INTEGER i = 2;" ,
                        "        while (i != 0) {" ,
                        "            PRINT(\"bean\");" ,
                        "            i = i - 1;" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testWhileBig(){
        Ast input = Parser.parse(Lexer.lex("LET i : INTEGER = 2;" +
                "\nLET n : INTEGER = 5;" +
                "\n LET zero: INTEGER;" +
                "\nWHILE n != 0 DO\n    zero = n - i * ( n / i );" +
                "\n    IF zero != 1 THEN\n    ELSE\n    END\n    IF zero != 1 THEN\n        PRINT(\"even\");" +
                "\n    ELSE\n    END\n    IF zero != 1 THEN\n    ELSE\n        PRINT(\"odd\");" +
                "\n    END\n    IF zero != 1 THEN\n        PRINT(\"even\");" +
                "\n    ELSE\n        PRINT(\"odd\");" +
                "\n    END\n    n = n - 1;\nEND"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        INTEGER i = 2;" ,
                        "        INTEGER n = 5;" ,
                        "        INTEGER zero;" ,
                        "        while (n != 0) {" ,
                        "            zero = n - i * (n / i);" ,
                        "            if (zero != 1) {}" ,
                        "            if (zero != 1) {" ,
                        "                PRINT(\"even\");" ,
                        "            }" ,
                        "            if (zero != 1) {} else {" ,
                        "                PRINT(\"odd\");" ,
                        "            }" ,
                        "            if (zero != 1) {" ,
                        "                PRINT(\"even\");" ,
                        "            } else {" ,
                        "                PRINT(\"odd\");" ,
                        "            }" ,
                        "            n = n - 1;" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testLiteral(){
        Ast input = Parser.parse(Lexer.lex("LET test : BOOLEAN = TRUE;\nIF test THEN\n    PRINT(\"success!\");\nELSE\n    PRINT(\"failure?\");\nEND"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        BOOLEAN test = true;" ,
                        "        if (test) {" ,
                        "            PRINT(\"success!\");" ,
                        "        } else {" ,
                        "            PRINT(\"failure?\");" ,
                        "        }" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();
        test(input, expected);
    }

    @Test
    void testGroup(){
        Ast input = Parser.parse(Lexer.lex("LET value : INTEGER = (2 * (10 - 3) + (5 / 2));"));
        String expected = String.join(System.lineSeparator(),
                "public final class Main {" ,
                        "" ,
                        "    public static void main(String[] args) {" ,
                        "        INTEGER value = (2 * (10 - 3) + (5 / 2));" ,
                        "    }" ,
                        "" ,
                        "}"
                ) + System.lineSeparator();
        test(input, expected);
    }


    private static <T extends Ast> void test(Ast input,String expected) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        Generator generator = new Generator(writer);
        if (expected != null) {
            generator.visit(input);
            Assertions.assertEquals(expected, out.toString());
        } else {
            System.out.println("failed");
        }
    }


}
