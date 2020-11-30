package plc.compiler;

import java.io.PrintWriter;

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
        newline(0);
        indent++;
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);

        int count = 0;
        while(count < ast.getStatements().size()){
            //print(ast.getStatements().get(count));
            visit(ast.getStatements().get(count));
            //print(";");
            newline(indent);
            count++;
        }

        indent--;
        newline(indent);
        print("}");
        newline(0);
        newline(0);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // TODO:  Generate Java to handle Expression node.
        //print(ast.getClass(), " ", ast.getExpression());

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

        // TODO:  Generate Java to handle Assignment node.

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        // TODO:  Generate Java to handle If node.

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        // TODO:  Generate Java to handle While node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        // TODO:  Generate Java to handle Literal node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        // TODO:  Generate Java to handle Group node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        // TODO:  Generate Java to handle Binary node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Variable ast) {

        // TODO:  Generate Java to handle Variable node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        // TODO:  Generate Java to handle Function node.

        return null;
    }

}
