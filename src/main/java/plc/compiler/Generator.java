package plc.compiler;

import javax.print.DocFlavor;
import java.io.PrintWriter;
import java.math.BigDecimal;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        // Source node
        print("public final class Main {");
        newline(indent);
        indent++;
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;

        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }

        indent--;
        newline(indent);
        print("}");
        indent--;
        newline(indent);
        newline(indent);
        print("}");
        newline(indent);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Declaration node
        print(ast.getType(), " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        print(ast.getName());
        print(" = ");
        visit(((Ast.Expression.Binary) ast.getExpression()).getLeft());
        print(" ", ((Ast.Expression.Binary) ast.getExpression()).getOperator(), " ");
        visit(((Ast.Expression.Binary) ast.getExpression()).getRight());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ") {");
        indent++;
        newline(indent);
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }
        indent--;
        newline(indent);
        print("}");
        if (ast.getElseStatements().size() > 0) {
            print(" else {");
            indent++;
            newline(indent);
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
            }
            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        // TODO:  Generate Java to handle While node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.equals("TRUE")) {
            print("true");
        } else if (ast.equals("FALSE")) {
            print("false");
        } else if (ast.getValue() instanceof String) {
            print("\"", ast.getValue(), "\"");
        } else {
            print(ast.getValue());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        print(ast.getLeft(), " ", ast.getOperator(), " " , ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Variable ast) {
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getName(), "(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i + 1 < ast.getArguments().size()) {
                print(", ");
            }
        }
        print(")");
        return null;
    }

}
