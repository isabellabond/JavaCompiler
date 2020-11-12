package plc.compiler;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    final CharStream chars;

    Lexer(String input) {
        chars = new CharStream(input);
    }

    public static List<Token> lex(String input) throws ParseException {
        return new Lexer(input).lex();
    }

    List<Token> lex() throws ParseException {
        List<Token> tokens = new ArrayList<Token>();
        while (chars.has(0)) {
            if (match("[ \\n\\r\\t]")) {
                chars.skip();
            }
            else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    Token lexToken() throws ParseException {
        if (peek("\"")) {
            return lexString();
        } else if (peek("[0-9]")) {
            return lexNumber();
        } else if (peek("[A-Za-z_]")){
            return lexIdentifier();
        } else {
            return lexOperator();
        }
    }

    Token lexIdentifier() throws ParseException {
        while (match("[A-Za-z0-9_]")) {};
        return chars.emit(Token.Type.IDENTIFIER);
    }

    Token lexNumber() throws ParseException {
        boolean decimalLexed = false;
        while (match("[0-9]")) {
            if (match("\\.","[0-9]")) {
                if (decimalLexed) {
                    throw new ParseException("Multiple '.' in number.", chars.index);
                }
                decimalLexed = true;
            }
        }

        if (decimalLexed) {
            return chars.emit(Token.Type.DECIMAL);
        }
        else {
            return chars.emit(Token.Type.INTEGER);
        }
    }

    Token lexString() throws ParseException {
        if (!match("\"")) {
            throw new ParseException("No leading quote in string.", chars.index);
        }
        while (match("[^\"]")) {};
        if (!match("\"")) {
            throw new ParseException("No closing quote in string.", chars.index);
        }

        return chars.emit(Token.Type.STRING);
    }

    Token lexOperator() throws ParseException {
        if (match("!") | match("=")) {
            match("=");
        } else {
            chars.advance();
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

     public static final class CharStream {

        final String input;
        int index = 0;
        int length = 0;

        CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset); //throws if out of bounds
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip(); //we've saved the starting point already
            return new Token(type, input.substring(start, index), start);
        }

    }
}
