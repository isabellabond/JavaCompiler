package plc.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public static Ast parse(List<Token> tokens) throws ParseException {
        return new Parser(tokens).parseSource();
    }

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while(tokens.has(0)){
            statements.add(parseStatement());
        }
        return new Ast.Source(statements);
    }

    public Ast.Statement parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        else if (peek(Token.Type.IDENTIFIER,"=")) {
            return parseAssignmentStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else {
            return parseExpressionStatement();
        }
    }

    public Ast.Statement.Expression parseExpressionStatement() throws ParseException {
        Ast.Expression expression = parseEqualityExpression();
        if (!match(";")) {
            throw new ParseException("expression statement not followed by ';'", tokens.index);
        }
        return new Ast.Statement.Expression(expression);
    }

    // declaration-statement ::= LET identifier : identifier ( = expression)? ;
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("'LET ' not followed by identifier", tokens.index);
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        if (!match(":")) {
            throw new ParseException("'LET identifier ' not followed by ':'", tokens.index);
        }
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("'LET identifier : ' not followed by identifier", tokens.index);
        }
        String type = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("LET statement not followed by ';'", tokens.index);
        }

        return new Ast.Statement.Declaration(name, type, value);
    }

    //assignment-statement ::= identifier = expression ;
    public Ast.Statement.Assignment parseAssignmentStatement() throws ParseException {
        // type checking done in calling function
        // identifier then '='
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        match("=");

        Ast.Expression ex = parseExpression();

        if (!match(";")) {
            throw new ParseException("assignment statement not followed by ';'", tokens.index);
        }

        return new Ast.Statement.Assignment(name, ex);
    }

    //if-statement ::= IF expression THEN statement* ( ELSE statement* )? END
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Expression condition = parseExpression();
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (!match("THEN")) {
            throw new ParseException("No THEN", tokens.index);
        }

        while (!match("END")) {
            thenStatements.add(parseStatement());
            if(match("ELSE")){
                while (!match("END")) {
                    elseStatements.add(parseStatement());
                }
                break;
            }
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    //while-statement ::= WHILE expression DO statement* END
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        Ast.Expression condition = parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();

        if (!match("DO")) {
            throw new ParseException("No DO", tokens.index);
        }

        while (!match("END")) {
            statements.add(parseStatement());
        }

        return new Ast.Statement.While(condition, statements);
    }

    public Ast.Expression parseExpression() throws ParseException {
        return parseEqualityExpression();
    }

    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();
        while(match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();
        while(match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();
        while(match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    // identifier ( ( (expression ( , expression )* )? ) )? |
    //    ( expression )
    //checking for token type
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        //these are definitely right!
        if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            literal = literal.substring(1,literal.length() - 1);
            literal = literal.replaceAll("\\\\\"", "\"");
            return new Ast.Expression.Literal(literal);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (match("(")) {
                List<Ast.Expression> expressions = new ArrayList<Ast.Expression>();
                while (!match(")")) {
                    match(",");
                    expressions.add(parseExpression());
                }
                return new Ast.Expression.Function(name, expressions);
            }
            return new Ast.Expression.Variable(name);
        } else if (match("(")) {
            Ast.Expression expression = parseExpression();
            if (!match(")")) {
                throw new ParseException("unclosed expression", tokens.index);
            }
            return new Ast.Expression.Group(expression);
        } else {
            throw new ParseException("invalid primary expression token", tokens.index);
        }


    }

    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError();
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
