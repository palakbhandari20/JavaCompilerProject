package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.Token;
import lexer.TokenType;
import parser.AST.*;

public class Parser {
    private List<Token> tokens;
    private int current;
    private int indentLevel = 0; // For prettier logging

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
        System.out.println("===== PARSER INITIALIZED =====");
    }

    private String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
    
    private void log(String message) {
        System.out.println(getIndent() + message);
    }

    public Program parseProgram() {
        log("BEGIN PARSING PROGRAM");
        indentLevel++;
        
        int line = peek().getLine();
        int column = peek().getColumn();
        Program program = new Program(line, column);

        // Parse class declaration
        log("Expecting class declaration");
        consume(TokenType.PUBLIC);
        consume(TokenType.CLASS);
        String className = consume(TokenType.IDENTIFIER).getLexeme();
        log("Found class: " + className);
        consume(TokenType.LEFT_BRACE);

        // Parse class members (in this case, just the main method)
        log("Parsing class members");
        indentLevel++;
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            FunctionDeclaration func = parseFunction();
            log("Added function: " + func.getName());
            program.addFunction(func);
        }
        indentLevel--;

        consume(TokenType.RIGHT_BRACE);
        
        indentLevel--;
        log("COMPLETED PARSING PROGRAM");
        
        // Print program summary
        System.out.println("\n===== PROGRAM SUMMARY =====");
        System.out.println("Class: " + className);
        System.out.println("Functions: " + program.getFunctions().size());
        for (FunctionDeclaration func : program.getFunctions()) {
            System.out.println("  - " + (func.isPublic() ? "public " : "") + 
                              (func.isStatic() ? "static " : "") + 
                              func.getReturnType().getName() + " " + 
                              func.getName() + "(" + formatParameters(func.getParameters()) + ")");
        }
        System.out.println("=========================");
        
        return program;
    }

    private String formatParameters(List<Parameter> parameters) {
        if (parameters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = parameters.get(i);
            sb.append(param.getType().getName()).append(" ").append(param.getName());
            if (i < parameters.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private FunctionDeclaration parseFunction() {
        log("BEGIN PARSING FUNCTION");
        indentLevel++;
        
        int line = peek().getLine();
        int column = peek().getColumn();

        // Parse modifiers (public, static)
        boolean isPublic = match(TokenType.PUBLIC);
        if (isPublic) log("Modifier: public");
        
        boolean isStatic = match(TokenType.STATIC);
        if (isStatic) log("Modifier: static");

        // Parse return type
        Type returnType = parseType();
        log("Return type: " + returnType.getName());
        
        // Parse function name
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        log("Function name: " + name);
        
        consume(TokenType.LEFT_PAREN);

        // Parse parameters
        log("Parsing parameters");
        indentLevel++;
        List<Parameter> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                Type paramType = parseType();
                
                // Handle array types - Check for array brackets BEFORE parameter name
                if (match(TokenType.LEFT_BRACKET)) {
                    consume(TokenType.RIGHT_BRACKET);
                    paramType = new Type(paramType.getName() + "[]", paramType.getLine(), paramType.getColumn());
                }
                
                String paramName = consume(TokenType.IDENTIFIER).getLexeme();
                log("Parameter: " + paramType.getName() + " " + paramName);
                parameters.add(new Parameter(paramType, paramName, line, column));
            } while (match(TokenType.COMMA));
        } else {
            log("No parameters");
        }
        indentLevel--;
        
        consume(TokenType.RIGHT_PAREN);

        // Parse function body
        log("Parsing function body");
        Block body = parseBlock();

        // Create function with modifiers
        FunctionDeclaration func = new FunctionDeclaration(returnType, name, parameters, body, line, column);
        func.setPublic(isPublic);
        func.setStatic(isStatic);
        
        indentLevel--;
        log("END PARSING FUNCTION: " + name);
        
        return func;
    }
    
    private Type parseType() {
        Token token = peek();
        if (match(TokenType.INT) || match(TokenType.FLOAT) || 
            match(TokenType.STRING) || match(TokenType.BOOLEAN) || 
            match(TokenType.VOID)) {
            return new Type(token.getLexeme(), token.getLine(), token.getColumn());
        }
        throw new RuntimeException("Expected type but got " + token.getType());
    }


    private Block parseBlock() {
        log("BEGIN PARSING BLOCK");
        indentLevel++;
        
        int line = peek().getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_BRACE);

        Block block = new Block(line, column);
        int statementCount = 0;
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            Statement stmt = parseStatement();
            block.addStatement(stmt);
            statementCount++;
            log("Added statement #" + statementCount + ": " + getStatementDescription(stmt));
        }

        consume(TokenType.RIGHT_BRACE);
        
        indentLevel--;
        log("END PARSING BLOCK (" + statementCount + " statements)");
        
        return block;
    }

    private String getStatementDescription(Statement stmt) {
        if (stmt instanceof ExpressionStatement) {
            return "Expression: " + getExpressionDescription(((ExpressionStatement) stmt).getExpression());
        } else if (stmt instanceof IfStatement) {
            return "If statement";
        } else if (stmt instanceof WhileStatement) {
            return "While statement";
        } else if (stmt instanceof ReturnStatement) {
            return "Return statement";
        } else if (stmt instanceof VarDeclarationStatement) {
            VarDeclaration decl = ((VarDeclarationStatement) stmt).getDeclaration();
            return "Variable declaration: " + decl.getType().getName() + " " + decl.getName();
        } else if (stmt instanceof Block) {
            return "Block statement";
        } else {
            return stmt.getClass().getSimpleName();
        }
    }

    private String getExpressionDescription(Expression expr) {
        if (expr instanceof Literal) {
            Literal lit = (Literal) expr;
            return "Literal(" + lit.getType() + "): " + lit.getValue();
        } else if (expr instanceof Variable) {
            return "Variable: " + ((Variable) expr).getName();
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expr;
            return "Binary expression: " + bin.getOperator();
        } else if (expr instanceof Assignment) {
            Assignment assign = (Assignment) expr;
            return "Assignment: " + assign.getVariable() + " = ...";
        } else if (expr instanceof Call) {
            Call call = (Call) expr;
            return "Function call: " + call.getName() + "()";
        } else {
            return expr.getClass().getSimpleName();
        }
    }

    private Statement parseStatement() {
        log("Parsing statement");
        indentLevel++;
        
        Statement result;
        switch (peek().getType()) {
            case IF:
                log("Found if statement");
                result = parseIfStatement();
                break;
            case WHILE:
                log("Found while statement");
                result = parseWhileStatement();
                break;
            case RETURN:
                log("Found return statement");
                result = parseReturnStatement();
                break;
            case INT:
            case FLOAT:
            case STRING:  
            case BOOLEAN:
                // Handle variable declarations that start with a type
                log("Found variable declaration");
                result = parseVarDeclarationStatement();
                break;
            case IDENTIFIER:
                // This branch might need more logic depending on your grammar
                if (peekNext().getType() == TokenType.IDENTIFIER) {
                    log("Found variable declaration with identifier");
                    result = parseVarDeclarationStatement();
                } else {
                    log("Found expression statement with identifier");
                    result = parseExpressionStatement();
                }
                break;
            case LEFT_BRACE:
                log("Found block statement");
                result = parseBlock();
                break;
            default:
                log("Found expression statement");
                result = parseExpressionStatement();
                break;
        }
        
        indentLevel--;
        return result;
    }

    private IfStatement parseIfStatement() {
        log("BEGIN PARSING IF STATEMENT");
        indentLevel++;
        
        int line = consume(TokenType.IF).getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_PAREN);
        
        log("Parsing condition");
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN);

        log("Parsing 'then' branch");
        Statement thenBranch = parseStatement();
        
        Statement elseBranch = null;
        if (match(TokenType.ELSE)) {
            log("Parsing 'else' branch");
            elseBranch = parseStatement();
        }

        indentLevel--;
        log("END PARSING IF STATEMENT");
        
        return new IfStatement(condition, thenBranch, elseBranch, line, column);
    }

    private WhileStatement parseWhileStatement() {
        log("BEGIN PARSING WHILE STATEMENT");
        indentLevel++;
        
        int line = consume(TokenType.WHILE).getLine();
        int column = peek().getColumn();
        consume(TokenType.LEFT_PAREN);
        
        log("Parsing condition");
        Expression condition = parseExpression();
        consume(TokenType.RIGHT_PAREN);

        log("Parsing loop body");
        Statement body = parseStatement();
        
        indentLevel--;
        log("END PARSING WHILE STATEMENT");
        
        return new WhileStatement(condition, body, line, column);
    }

    private ReturnStatement parseReturnStatement() {
        log("BEGIN PARSING RETURN STATEMENT");
        indentLevel++;
        
        Token token = consume(TokenType.RETURN);
        
        log("Parsing return value");
        Expression value = parseExpression();
        consume(TokenType.SEMICOLON);
        
        indentLevel--;
        log("END PARSING RETURN STATEMENT");
        
        return new ReturnStatement(value, token.getLine(), token.getColumn());
    }

    private VarDeclarationStatement parseVarDeclarationStatement() {
        log("BEGIN PARSING VARIABLE DECLARATION");
        indentLevel++;
        
        Type type = parseType();
        log("Variable type: " + type.getName());
        
        Token nameToken = consume(TokenType.IDENTIFIER);
        log("Variable name: " + nameToken.getLexeme());
        
        Expression initializer = null;
        if (match(TokenType.ASSIGN)) {
            log("Parsing initializer");
            initializer = parseExpression();
        }

        consume(TokenType.SEMICOLON);
        
        VarDeclaration declaration = new VarDeclaration(type, nameToken.getLexeme(), initializer,
                nameToken.getLine(), nameToken.getColumn());
        
        indentLevel--;
        log("END PARSING VARIABLE DECLARATION: " + type.getName() + " " + nameToken.getLexeme());
        
        return new VarDeclarationStatement(declaration, nameToken.getLine(), nameToken.getColumn());
    }

    private ExpressionStatement parseExpressionStatement() {
        log("BEGIN PARSING EXPRESSION STATEMENT");
        indentLevel++;
        
        Expression expr = parseExpression();
        Token token = consume(TokenType.SEMICOLON);
        
        indentLevel--;
        log("END PARSING EXPRESSION STATEMENT");
        
        return new ExpressionStatement(expr, token.getLine(), token.getColumn());
    }

    private Expression parseExpression() {
        log("Parsing expression");
        indentLevel++;
        Expression result = parseAssignment();
        indentLevel--;
        return result;
    }

    private Expression parseAssignment() {
        log("Parsing assignment");
        indentLevel++;
        
        Expression expr = parseEquality();
        if (match(TokenType.ASSIGN)) {
            log("Found assignment operator");
            if (expr instanceof Variable) {
                String name = ((Variable) expr).getName();
                log("Assignment target: " + name);
                Expression value = parseAssignment();
                expr = new Assignment(name, value, expr.getLine(), expr.getColumn());
            } else {
                throw new RuntimeException("Invalid assignment target at line " + expr.getLine());
            }
        }
        
        indentLevel--;
        return expr;
    }

    private Expression parseEquality() {
        log("Parsing equality");
        indentLevel++;
        
        Expression expr = parseComparison();
        while (match(TokenType.EQUAL) || match(TokenType.NOT_EQUAL)) {
            String operator = previous().getLexeme();
            log("Found equality operator: " + operator);
            Expression right = parseComparison();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        
        indentLevel--;
        return expr;
    }

    private Expression parseComparison() {
        log("Parsing comparison");
        indentLevel++;
        
        Expression expr = parseAdditive();
        while (match(TokenType.LT) || match(TokenType.GT) || match(TokenType.LTE) || match(TokenType.GTE)) {
            String operator = previous().getLexeme();
            log("Found comparison operator: " + operator);
            Expression right = parseAdditive();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        
        indentLevel--;
        return expr;
    }

    private Expression parseAdditive() {
        log("Parsing additive expression");
        indentLevel++;
        
        Expression expr = parseMultiplicative();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            String operator = previous().getLexeme();
            log("Found additive operator: " + operator);
            Expression right = parseMultiplicative();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        
        indentLevel--;
        return expr;
    }

    private Expression parseMultiplicative() {
        log("Parsing multiplicative expression");
        indentLevel++;
        
        Expression expr = parsePrimary();
        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE)) {
            String operator = previous().getLexeme();
            log("Found multiplicative operator: " + operator);
            Expression right = parsePrimary();
            expr = new BinaryExpression(expr, operator, right, expr.getLine(), expr.getColumn());
        }
        
        indentLevel--;
        return expr;
    }

    private Expression parsePrimary() {
        log("Parsing primary expression");
        indentLevel++;
        
        Token token = peek();
        Expression result;
        
        switch (token.getType()) {
            case INTEGER_LITERAL:
                advance();
                log("Found integer literal: " + token.getLexeme());
                result = new Literal(Integer.parseInt(token.getLexeme()), "int", token.getLine(), token.getColumn());
                break;
            case FLOAT_LITERAL:
                advance();
                log("Found float literal: " + token.getLexeme());
                result = new Literal(Float.parseFloat(token.getLexeme()), "float", token.getLine(), token.getColumn());
                break;
            case STRING_LITERAL:
                advance();
                log("Found string literal: " + token.getLexeme());
                result = new Literal(token.getLexeme(), "string", token.getLine(), token.getColumn());
                break;
            case IDENTIFIER:
                advance();
                if (match(TokenType.LEFT_PAREN)) {
                    log("Found function call: " + token.getLexeme() + "()");
                    List<Expression> arguments = new ArrayList<>();
                    if (!check(TokenType.RIGHT_PAREN)) {
                        log("Parsing function arguments");
                        indentLevel++;
                        int argCount = 0;
                        do {
                            argCount++;
                            log("Parsing argument #" + argCount);
                            arguments.add(parseExpression());
                        } while (match(TokenType.COMMA));
                        indentLevel--;
                    } else {
                        log("No arguments");
                    }
                    consume(TokenType.RIGHT_PAREN);
                    result = new Call(token.getLexeme(), arguments, token.getLine(), token.getColumn());
                } else {
                    log("Found variable: " + token.getLexeme());
                    result = new Variable(token.getLexeme(), token.getLine(), token.getColumn());
                }
                break;
            case LEFT_PAREN:
                advance();
                log("Found parenthesized expression");
                result = parseExpression();
                consume(TokenType.RIGHT_PAREN);
                break;
            default:
                throw new RuntimeException("Unexpected token: " + token.getType() +
                        " at line " + token.getLine());
        }
        
        indentLevel--;
        return result;
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
        throw new RuntimeException("Expected token " + type + " but got " + peek().getType() + 
                                  " at line " + peek().getLine() + ", column " + peek().getColumn());
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