package plc.compiler;

import java.math.BigDecimal;
import java.util.List;
import java.math.BigInteger;
import java.util.Optional;

public final class Analyzer implements Ast.Visitor<Ast> {

    public Scope scope;

    public Analyzer(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Ast visit(Ast.Source ast) throws AnalysisException {
        if (ast.getStatements().isEmpty())
            throw new AnalysisException("Source statements empty");
        return new Ast.Source(ast.getStatements());
    }

    private Ast.Statement visit(Ast.Statement ast) throws AnalysisException {
        return (Ast.Statement) visit((Ast) ast);
    }

    @Override
    public Ast.Statement.Expression visit(Ast.Statement.Expression ast) throws AnalysisException {
        if (ast.getExpression().getClass() != Ast.Expression.Function.class)
            throw new AnalysisException("Expression not function");
        return new Ast.Statement.Expression(ast.getExpression());
    }

    @Override
    public Ast.Statement.Declaration visit(Ast.Statement.Declaration ast) throws AnalysisException {

        scope.define(ast.getName(), Stdlib.getType(ast.getType()));

        if (Stdlib.getType(ast.getType()).equals(Stdlib.Type.VOID)){
            throw new AnalysisException("Declaration type void");
        }

            //checkAssignable(ast.getValue().get().getType(), Stdlib.getType(ast.getType()));
            // Stdlib.getType(ast.getType()).getJvmName()
            return new Ast.Statement.Declaration(ast.getName(), Stdlib.getType(ast.getType()).getJvmName(),
                    ast.getValue().equals(Optional.empty()) ? Optional.empty() : Optional.of(visit(ast.getValue().get())));


//        try {
//            scope.lookup(ast.getName());
//        }
//        catch (AnalysisException e) {
//            if (Stdlib.getType(ast.getType()).equals(Stdlib.Type.VOID))
//                throw new AnalysisException("Declaration type void");
//            checkAssignable(ast.getValue().get().getType(), Stdlib.getType(ast.getType()));
//            return new Ast.Statement.Declaration(ast.getName(), Stdlib.getType(ast.getType()).getJvmName(), ast.getValue());
//        }
//        throw new AnalysisException("declared variable already defined");
    }

    @Override
    public Ast.Statement.Assignment visit(Ast.Statement.Assignment ast) throws AnalysisException {
        checkAssignable(ast.getExpression().getType(), scope.lookup(ast.getName()));
        return new Ast.Statement.Assignment(ast.getName(), ast.getExpression());
    }

    @Override
    public Ast.Statement.If visit(Ast.Statement.If ast) throws AnalysisException {
        if(!ast.getCondition().equals(Stdlib.Type.BOOLEAN))
            throw new AnalysisException("if condition not boolean");
        if (ast.getThenStatements().isEmpty())
            throw new AnalysisException("then statements empty");
        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }
        scope = scope.getParent();
        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getElseStatements()) {
            visit(statement);
        }
        scope = scope.getParent();
        return new Ast.Statement.If(ast.getCondition(), ast.getThenStatements(), ast.getElseStatements());
    }

    @Override
    public Ast.Statement.While visit(Ast.Statement.While ast) throws AnalysisException {
        if (!ast.getCondition().equals(Stdlib.Type.BOOLEAN))
            throw new AnalysisException("while condition not boolean");
        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        scope = scope.getParent();
        return new Ast.Statement.While(ast.getCondition(), ast.getStatements());
    }

    private Ast.Expression visit(Ast.Expression ast) throws AnalysisException {
        return (Ast.Expression) visit((Ast) ast);
    }

    @Override
    public Ast.Expression.Literal visit(Ast.Expression.Literal ast) throws AnalysisException {
        if (ast.getValue() instanceof Boolean) {
            return new Ast.Expression.Literal(Stdlib.Type.BOOLEAN, ast.getValue());
        } else if (ast.getValue() instanceof java.math.BigInteger) {
            BigInteger val = ((BigInteger) ast.getValue()).abs();
            BigInteger intMax = new BigInteger("2147483647");
            if(val.compareTo(intMax) > 0){
                throw new AnalysisException("Out of bounds");
            }
            else{
                return new Ast.Expression.Literal(Stdlib.Type.INTEGER, ((BigInteger) ast.getValue()).intValue());
            }
        } else if (ast.getValue() instanceof java.math.BigDecimal) {
            if( ((BigDecimal) ast.getValue()).abs().doubleValue() > Math.pow(10,31)-1) {
                throw new AnalysisException("Out of bounds");
            }
            else{
                return new Ast.Expression.Literal(((BigDecimal) ast.getValue()).doubleValue());
            }
        } else if (ast.getValue() instanceof String) {
            // Can only contain [A-Za-z0-9_!?.+-/* ]
            if(((String) ast.getValue()).contains("$%^#&(),<>")){
                throw new AnalysisException("Invalid characters");
            }
            else {
                return new Ast.Expression.Literal(Stdlib.Type.STRING, ast.getValue());
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Ast.Expression.Group visit(Ast.Expression.Group ast) throws AnalysisException {
        return new Ast.Expression.Group(visit(ast.getExpression()));
    }

    @Override
    public Ast.Expression.Binary visit(Ast.Expression.Binary ast) throws AnalysisException {
            return new Ast.Expression.Binary(ast.getOperator(), ast.getLeft(), ast.getRight());
    }

    @Override
    public Ast.Expression.Variable visit(Ast.Expression.Variable ast) throws AnalysisException {
        return new Ast.Expression.Variable(scope.lookup(ast.getName()), ast.getName());
    }

    @Override
    public Ast.Expression.Function visit(Ast.Expression.Function ast) throws AnalysisException {
        Stdlib.Function function = Stdlib.getFunction(ast.getName(), ast.getArguments().size());
        List<Stdlib.Type> paramTypes = function.getParameterTypes();
        for (int i = 0 ; i < paramTypes.size(); i++) {
            if (paramTypes.get(i).equals(ast.getArguments().get(i)))
                throw new AnalysisException("function args not of correct type");
        }
        return new Ast.Expression.Function(function.getJvmName(), ast.getArguments());
    }

    /**
     * Throws an AnalysisException if the first type is NOT assignable to the target type. * A type is assignable if and only if one of the following is true:
     *  - The types are equal, as according to Object#equals
     *  - The first type is an INTEGER and the target type is DECIMAL
     *  - The first type is not VOID and the target type is ANY
     */
    public static void checkAssignable(Stdlib.Type type, Stdlib.Type target) throws AnalysisException {
        if (type.equals(target)) return;
        if (type.equals(Stdlib.Type.INTEGER) && type.equals(Stdlib.Type.INTEGER)) return;
        if (!type.equals(Stdlib.Type.VOID) && type.equals(Stdlib.Type.ANY)) return;
        throw new AnalysisException("Target not assignable by type");
    }

}
