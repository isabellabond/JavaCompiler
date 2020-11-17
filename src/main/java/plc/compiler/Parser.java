package plc.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the tokens and returns the parsed AST.
     */
    public static Ast parse(List<Token> tokens) throws ParseException {
        return new Parser(tokens).parseSource();
    }

    /**
     * Parses the {@code source} rule.
     */

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while(tokens.has(0)){
            statements.add(parseStatement());
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, assignment, if, or while
     * statement, then it is an expression statement. See these methods for
     * clarification on what starts each type of statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(match("LET")) {
            // declaration-statement
            return parseDeclarationStatement();
        }
        else if(match(tokens.get(0).getType() == Token.Type.IDENTIFIER)){
            parseDeclarationStatement();
        }
        else if(match("IF")){
            // if-statement
            return parseIfStatement();
        }
        else if(match("WHILE")){
            // while-statement
            return parseIfStatement();
        }
        else{
            return parseExpressionStatement();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the {@code expression-statement} rule. This method is called if
     * the next tokens do not start another statement type, as explained in the
     * javadocs of {@link #parseStatement()}.
     */

    public Ast.Statement.Expression parseExpressionStatement() throws ParseException {
        Ast.Expression value = new Ast.Expression();
        if(value.equals(null)){
            throw new ParseException("Empty", tokens.index);
        }
        while (!match(";")) {
            value = parseEqualityExpression();
        }
        return new Ast.Statement.Expression(value);
    }

    /**
     * Parses the {@code declaration-statement} rule. This method should only be
     * called if the next tokens start a declaration statement, aka {@code let}.
     */

    // declaration-statement ::= LET identifier : identifier ( = expression)? ;
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");

        //not sure
        String name = tokens.get(0).getLiteral();

        match(":");

        String type = tokens.get(0).getLiteral();

        Optional<Ast.Expression> value = null;

        if(match("=")){
            value = Optional.of(parseExpression());
        }
        match(";");

        return new Ast.Statement.Declaration(name, type, value);

    }

    /**
     * Parses the {@code assignment-statement} rule. This method should only be
     * called if the next tokens start an assignment statement, aka both an
     * {@code identifier} followed by {@code =}.
     */

    //assignment-statement ::= identifier = expression ;
    public Ast.Statement.Assignment parseAssignmentStatement() throws ParseException {
        String name = tokens.get(0).getLiteral();
        match("=");
        Ast.Expression ex = parseExpression();

        return new Ast.Statement.Assignment(name, ex);
    }

    /**
     * Parses the {@code if-statement} rule. This method should only be called
     * if the next tokens start an if statement, aka {@code if}.
     */

    //if-statement ::= IF expression THEN statement* ( ELSE statement* )? END
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression condition = parseExpression();
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();
        if (!match("THEN")) {
            throw new ParseException("No THEN", tokens.index);
        }
        /*
        IF true THEN
            ...
        END
            ...
        END
         */
        while (!match("END")) {
            thenStatements.add(parseStatement());
            if(match("ELSE")){
                while (!match("END")) {
                    elseStatements.add(parseStatement());
                }
            }
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses the {@code while-statement} rule. This method should only be
     * called if the next tokens start a while statement, aka {@code while}.
     */

    //while-statement ::= WHILE expression DO statement* END
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
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

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseEqualityExpression();
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression left = parseAdditiveExpression();
        while(match("==") || match("!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression ex = parseAdditiveExpression();
            left = new Ast.Expression.Binary(operator, left, ex);
        }
        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left = parseMultiplicativeExpression();
        while(match("+") || match("-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left = parsePrimaryExpression();
        while(match("*") || match("/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */

    // identifier ( ( (expression ( , expression )* )? ) )? |
    //    ( expression )
    //checking for token type
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        //these are definitely right!
        if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        if (match("FALSE")) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        }

        //not sure about this one
        if(match(tokens.get(0).getType() == Token.Type.DECIMAL)){
            return new Ast.Expression.Literal(tokens.get(-1).getLiteral());
        }


    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
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
