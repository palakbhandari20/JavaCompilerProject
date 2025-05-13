package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.Token;
import lexer.TokenType;
import parser.AST.*;

public class Parser {
    private List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    public Program parseProgram() {
        int line = peek().getLine();
        int column = peek().getColumn();
        Program program = new Program(line, column);

        while (!isAtEnd()) {
            program.addFunction(parseFunction());
        }

        return program;
    }

    private FunctionDeclaration parseFunction() {
        int line = peek().getLine();
        int column = peek().getColumn();

        Type returnType = parseType();
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.LEFT_PAREN);

        List<Parameter> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Type paramType = parseType();
                String paramName = consume(TokenType.IDENTIFIER).getLexeme();
                parameters.add(new Parameter(paramType, paramName, line, column));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN);
        Block body = parseBlock();

        return new FunctionDeclaration(returnType, name, parameters, body, line, column);
    }

    private Type parseType() {
        Token token = consume(TokenType.IDENTIFIER);
        return new Type(token.getLexeme(), token.getLine(), token.getColumn());
    }

    private Block parseBlock() {
        int line = peek().getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_BRACE);

        Block block = new Block(line, column);
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            block.addStatement(parseStatement());
        }

        consume(TokenType.RIGHT_BRACE);
        return block;
    }

    private Statement parseStatement() {
        switch (peek().getType()) {
            case IF:
                return parseIfStatement();
            case WHILE:
                return parseWhileStatement();
            case RETURN:
                return parseReturnStatement();
            case IDENTIFIER:
                if (peekNext().getType() == TokenType.IDENTIFIER) {
                    return parseVarDeclarationStatement();
                } else {
                    return parseExpressionStatement();
                }
            case LEFT_BRACE:
                return parseBlock();
            default:
                return parseExpressionStatement();
        }
    }

    private IfStatement parseIfStatement() {
        int line = consume(TokenType.IF).getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_PAREN);
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN);

        Statement thenBranch = parseStatement();
        Statement elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }

        return new IfStatement(condition, thenBranch, elseBranch, line, column);
    }

    private WhileStatement parseWhileStatement() {
        int line = consume(TokenType.WHILE).getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_PAREN);
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN);

        Statement body = parseStatement();
        return new WhileStatement(condition, body, line, column);
    }

    private ReturnStatement parseReturnStatement() {
        Token token = consume(TokenType.RETURN);
        Expression value = parseExpression();
        consume(TokenType.SEMICOLON);
        return new ReturnStatement(value, token.getLine(), token.getColumn());
    }

    private VarDeclarationStatement parseVarDeclarationStatement() {
        Type type = parseType();
        Token nameToken = consume(TokenType.IDENTIFIER);
        Expression initializer = null;

        if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }

        consume(TokenType.SEMICOLON);
        VarDeclaration declaration = new VarDeclaration(type, nameToken.getLexeme(), initializer,
                nameToken.getLine(), nameToken.getColumn());
        return new VarDeclarationStatement(declaration, nameToken.getLine(), nameToken.getColumn());
    }

    private ExpressionStatement parseExpressionStatement() {
        Expression expr = parseExpression();
        Token token = consume(TokenType.SEMICOLON);
        return new ExpressionStatement(expr, token.getLine(), token.getColumn());
    }

    private Expression parseExpression() {
        return parseAssignment();
    }

    private Expression parseAssignment() {
        Expression expr = parseEquality();
        if (match(TokenType.ASSIGN)) {
            if (expr instanceof Variable) {
                String name = ((Variable) expr).getName();
                Expression value = parseAssignment();
                return new Assignment(name, value, expr.getLine(), expr.getColumn());
            } else {
                throw new RuntimeException("Invalid assignment target at line " + expr.getLine());
            }
        }
        return expr;
    }

    private Expression parseEquality() {
        Expression expr = parseComparison();
        while (match(TokenType.EQUAL) || match(TokenType.NOT_EQUAL)) {
            String operator = previous().getLexeme();
            Expression right = parseComparison();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        return expr;
    }

    private Expression parseComparison() {
        Expression expr = parseAdditive();
        while (match(TokenType.LT) || match(TokenType.GT) || match(TokenType.LTE) || match(TokenType.GTE)) {
            String operator = previous().getLexeme();
            Expression right = parseAdditive();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        return expr;
    }

    private Expression parseAdditive() {
        Expression expr = parseMultiplicative();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            String operator = previous().getLexeme();
            Expression right = parseMultiplicative();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        return expr;
    }

    private Expression parseMultiplicative() {
        Expression expr = parsePrimary();
        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE)) {
            String operator = previous().getLexeme();
            Expression right = parsePrimary();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        return expr;
    }

    private Expression parsePrimary() {
        Token token = peek();
        switch (token.getType()) {
            case INTEGER_LITERAL:
                advance();
                return new Literal(Integer.parseInt(token.getLexeme()), "int", token.getLine(), token.getColumn());
            case FLOAT_LITERAL:
                advance();
                return new Literal(Float.parseFloat(token.getLexeme()), "float", token.getLine(), token.getColumn());
            case STRING_LITERAL:
                advance();
                return new Literal(token.getLexeme(), "string", token.getLine(), token.getColumn());
            case IDENTIFIER:
                advance();
                if (match(TokenType.LEFT_PAREN)) {
                    List<Expression> arguments = new ArrayList<>();
                    if (!check(TokenType.RIGHT_PAREN)) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RIGHT_PAREN);
                    return new Call(token.getLexeme(), arguments, token.getLine(), token.getColumn());
                }
                return new Variable(token.getLexeme(), token.getLine(), token.getColumn());
            case LEFT_PAREN:
                advance();
                Expression expr = parseExpression();
                consume(TokenType.RIGHT_PAREN);
                return expr;
            default:
                throw new RuntimeException("Unexpected token: " + token.getType() +
                        " at line " + token.getLine());
        }
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type) {
        if (check(type)) return advance();
        throw new RuntimeException("Expected token " + type + " but got " + peek().getType());
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        if (current + 1 >= tokens.size()) return tokens.get(current);
        return tokens.get(current + 1);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
} 
