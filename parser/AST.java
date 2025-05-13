package parser;

import java.util.ArrayList;
import java.util.List;

public class AST {
    // Base node that all AST nodes extend
    public static abstract class Node {
        private int line;
        private int column;

        public Node(int line, int column) {
            this.line = line;
            this.column = column;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }
    }

    // Program is the root node of our AST
    public static class Program extends Node {
        private List<FunctionDeclaration> functions;

        public Program(int line, int column) {
            super(line, column);
            this.functions = new ArrayList<>();
        }

        public void addFunction(FunctionDeclaration function) {
            functions.add(function);
        }

        public List<FunctionDeclaration> getFunctions() {
            return functions;
        }
    }

    // Type of a variable or function
    public static class Type extends Node {
        private String name;

        public Type(String name, int line, int column) {
            super(line, column);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Variable declaration
    public static class VarDeclaration extends Node {
        private Type type;
        private String name;
        private Expression initialValue;

        public VarDeclaration(Type type, String name, Expression initialValue, int line, int column) {
            super(line, column);
            this.type = type;
            this.name = name;
            this.initialValue = initialValue;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Expression getInitialValue() {
            return initialValue;
        }
    }

    // Function declaration
    public static class FunctionDeclaration extends Node {
        private Type returnType;
        private String name;
        private List<Parameter> parameters;
        private Block body;

        public FunctionDeclaration(Type returnType, String name, List<Parameter> parameters, 
                                   Block body, int line, int column) {
            super(line, column);
            this.returnType = returnType;
            this.name = name;
            this.parameters = parameters;
            this.body = body;
        }

        public Type getReturnType() {
            return returnType;
        }

        public String getName() {
            return name;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public Block getBody() {
            return body;
        }
        // Add to FunctionDeclaration class
private boolean isPublic;
private boolean isStatic;

// Add these methods
public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
}

public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
}

public boolean isPublic() {
    return isPublic;
}

public boolean isStatic() {
    return isStatic;
}
    }

    // Function parameter
    public static class Parameter extends Node {
        private Type type;
        private String name;

        public Parameter(Type type, String name, int line, int column) {
            super(line, column);
            this.type = type;
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    // Base class for all statements
    public static abstract class Statement extends Node {
        public Statement(int line, int column) {
            super(line, column);
        }
    }

    // Block of statements
    public static class Block extends Statement {
        private List<Statement> statements;

        public Block(int line, int column) {
            super(line, column);
            this.statements = new ArrayList<>();
        }

        public void addStatement(Statement statement) {
            statements.add(statement);
        }

        public List<Statement> getStatements() {
            return statements;
        }
    }

    // Expression statement
    public static class ExpressionStatement extends Statement {
        private Expression expression;

        public ExpressionStatement(Expression expression, int line, int column) {
            super(line, column);
            this.expression = expression;
        }

        public Expression getExpression() {
            return expression;
        }
    }

    // If statement
    public static class IfStatement extends Statement {
        private Expression condition;
        private Statement thenBranch;
        private Statement elseBranch;

        public IfStatement(Expression condition, Statement thenBranch, Statement elseBranch, int line, int column) {
            super(line, column);
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        public Expression getCondition() {
            return condition;
        }

        public Statement getThenBranch() {
            return thenBranch;
        }

        public Statement getElseBranch() {
            return elseBranch;
        }
    }

    // While statement
    public static class WhileStatement extends Statement {
        private Expression condition;
        private Statement body;

        public WhileStatement(Expression condition, Statement body, int line, int column) {
            super(line, column);
            this.condition = condition;
            this.body = body;
        }

        public Expression getCondition() {
            return condition;
        }

        public Statement getBody() {
            return body;
        }
    }

    // Return statement
    public static class ReturnStatement extends Statement {
        private Expression value;

        public ReturnStatement(Expression value, int line, int column) {
            super(line, column);
            this.value = value;
        }

        public Expression getValue() {
            return value;
        }
    }

    // Variable declaration statement
    public static class VarDeclarationStatement extends Statement {
        private VarDeclaration declaration;

        public VarDeclarationStatement(VarDeclaration declaration, int line, int column) {
            super(line, column);
            this.declaration = declaration;
        }

        public VarDeclaration getDeclaration() {
            return declaration;
        }
    }

    // Base class for all expressions
    public static abstract class Expression extends Node {
        public Expression(int line, int column) {
            super(line, column);
        }
    }

    // Literal expression
    public static class Literal extends Expression {
        private Object value;
        private String type; // "int", "float", "string"

        public Literal(Object value, String type, int line, int column) {
            super(line, column);
            this.value = value;
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public String getType() {
            return type;
        }
    }

    // Variable reference expression
    public static class Variable extends Expression {
        private String name;

        public Variable(String name, int line, int column) {
            super(line, column);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Binary operation expression
    public static class BinaryExpression extends Expression {
        private Expression left;
        private String operator;
        private Expression right;

        public BinaryExpression(Expression left, String operator, Expression right, int line, int column) {
            super(line, column);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        public Expression getLeft() {
            return left;
        }

        public String getOperator() {
            return operator;
        }

        public Expression getRight() {
            return right;
        }
    }

    // Assignment expression
    public static class Assignment extends Expression {
        private String variable;
        private Expression value;

        public Assignment(String variable, Expression value, int line, int column) {
            super(line, column);
            this.variable = variable;
            this.value = value;
        }

        public String getVariable() {
            return variable;
        }

        public Expression getValue() {
            return value;
        }
    }

    // Function call expression
    public static class Call extends Expression {
        private String name;
        private List<Expression> arguments;

        public Call(String name, List<Expression> arguments, int line, int column) {
            super(line, column);
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public List<Expression> getArguments() {
            return arguments;
        }
    }
}
