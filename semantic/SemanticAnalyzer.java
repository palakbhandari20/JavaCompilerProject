package semantic;

import parser.AST.*;
import semantic.SymbolTable.Symbol;
import semantic.SymbolTable.SymbolKind;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private List<String> errors;
    private String currentFunction;
    
    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
        this.currentFunction = null;
    }
    
    public void analyze(Program program) {
        // First pass: Declare all functions
        for (FunctionDeclaration function : program.getFunctions()) {
            declareFunction(function);
        }
        
        // Second pass: Check function bodies
        for (FunctionDeclaration function : program.getFunctions()) {
            analyzeFunction(function);
        }
        
        // Report any errors found during analysis
        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Semantic errors:\n");
            for (String error : errors) {
                errorMessage.append("- ").append(error).append("\n");
            }
            throw new RuntimeException(errorMessage.toString());
        }
    }
    
    private void declareFunction(FunctionDeclaration function) {
        String name = function.getName();
        String returnType = function.getReturnType().getName();
        
        // Check for duplicate function declarations
        Symbol existing = symbolTable.resolve(name);
        if (existing != null) {
            addError(function, "Function '" + name + "' already declared");
            return;
        }
        
        // Add function to symbol table
        symbolTable.define(new Symbol(name, returnType, SymbolKind.FUNCTION));
    }
    
    private void analyzeFunction(FunctionDeclaration function) {
        String name = function.getName();
        currentFunction = name;
        
        // Create a new scope for the function
        symbolTable.enterScope();
        
        // Declare parameters
        for (Parameter param : function.getParameters()) {
            String paramName = param.getName();
            String paramType = param.getType().getName();
            
            // Check for duplicate parameter names
            Symbol existing = symbolTable.resolve(paramName);
            if (existing != null && symbolTable.isInGlobalScope()) {
                addError(param, "Parameter '" + paramName + "' already declared");
                continue;
            }
            
            symbolTable.define(new Symbol(paramName, paramType, SymbolKind.PARAMETER));
        }
        
        // Analyze function body
        analyzeBlock(function.getBody());
        
        // Exit function scope
        symbolTable.exitScope();
        currentFunction = null;
    }
    
    private void analyzeBlock(Block block) {
        // Create a new scope for the block
        symbolTable.enterScope();
        
        // Analyze each statement in the block
        for (Statement statement : block.getStatements()) {
            analyzeStatement(statement);
        }
        
        // Exit block scope
        symbolTable.exitScope();
    }
    
    private void analyzeStatement(Statement statement) {
        if (statement instanceof ExpressionStatement) {
            analyzeExpression(((ExpressionStatement) statement).getExpression());
        } else if (statement instanceof IfStatement) {
            analyzeIfStatement((IfStatement) statement);
        } else if (statement instanceof WhileStatement) {
            analyzeWhileStatement((WhileStatement) statement);
        } else if (statement instanceof ReturnStatement) {
            analyzeReturnStatement((ReturnStatement) statement);
        } else if (statement instanceof VarDeclarationStatement) {
            analyzeVarDeclaration(((VarDeclarationStatement) statement).getDeclaration());
        } else if (statement instanceof Block) {
            analyzeBlock((Block) statement);
        }
    }
    
    private void analyzeIfStatement(IfStatement statement) {
        Expression condition = statement.getCondition();
        analyzeExpression(condition);
        
        // Analyze the 'then' branch
        analyzeStatement(statement.getThenBranch());
        
        // Analyze the 'else' branch if it exists
        if (statement.getElseBranch() != null) {
            analyzeStatement(statement.getElseBranch());
        }
    }
    
    private void analyzeWhileStatement(WhileStatement statement) {
        Expression condition = statement.getCondition();
        analyzeExpression(condition);
        
        // Analyze the loop body
        analyzeStatement(statement.getBody());
    }
    
    private void analyzeReturnStatement(ReturnStatement statement) {
        // Check if we're in a function
        if (currentFunction == null) {
            addError(statement, "Return statement outside of function");
            return;
        }
        
        // Get the function's return type
        Symbol function = symbolTable.resolve(currentFunction);
        if (function == null) {
            addError(statement, "Unknown function '" + currentFunction + "'");
            return;
        }
        
        String returnType = function.getType();
        Expression value = statement.getValue();
        
        // Check if the return type matches
        if ("void".equals(returnType) && value != null) {
            addError(statement, "Cannot return a value from a void function");
        } else if (!"void".equals(returnType) && value == null) {
            addError(statement, "Function must return a value of type '" + returnType + "'");
        } else if (value != null) {
            analyzeExpression(value);
            String valueType = getExpressionType(value);
            if (!returnType.equals(valueType) && !isCompatibleType(returnType, valueType)) {
                addError(statement, "Return type mismatch: expected '" + returnType + 
                        "', got '" + valueType + "'");
            }
        }
    }
    
    private void analyzeVarDeclaration(VarDeclaration declaration) {
        String name = declaration.getName();
        String type = declaration.getType().getName();
        
        // Check for duplicate variable declarations in the same scope
        Symbol existing = symbolTable.resolve(name);
        if (existing != null && !symbolTable.isInGlobalScope()) {
            addError(declaration, "Variable '" + name + "' already declared");
            return;
        }
        
        // Check the initializer if present
        Expression initialValue = declaration.getInitialValue();
        if (initialValue != null) {
            analyzeExpression(initialValue);
            String valueType = getExpressionType(initialValue);
            if (!type.equals(valueType) && !isCompatibleType(type, valueType)) {
                addError(declaration, "Cannot initialize variable of type '" + type + 
                        "' with value of type '" + valueType + "'");
            }
        }
        
        // Add variable to symbol table
        symbolTable.define(new Symbol(name, type, SymbolKind.VARIABLE));
    }
    
    private void analyzeExpression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            analyzeBinaryExpression((BinaryExpression) expression);
        } else if (expression instanceof Assignment) {
            analyzeAssignment((Assignment) expression);
        } else if (expression instanceof Variable) {
            analyzeVariable((Variable) expression);
        } else if (expression instanceof Call) {
            analyzeCall((Call) expression);
        } else if (expression instanceof Literal) {
            // No analysis needed for literals
        }
    }
    
    private void analyzeBinaryExpression(BinaryExpression expression) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();
        String operator = expression.getOperator();
        
        analyzeExpression(left);
        analyzeExpression(right);
        
        String leftType = getExpressionType(left);
        String rightType = getExpressionType(right);
        
        // Check if the operator is valid for the types
        if (operator.equals("+") || operator.equals("-") || 
            operator.equals("*") || operator.equals("/")) {
            // Arithmetic operators require numeric types
            if (!isNumericType(leftType)) {
                addError(expression, "Left operand of '" + operator + "' must be numeric");
            }
            if (!isNumericType(rightType)) {
                addError(expression, "Right operand of '" + operator + "' must be numeric");
            }
        } else if (operator.equals("==") || operator.equals("!=")) {
            // Equality operators require compatible types
            if (!leftType.equals(rightType) && !isCompatibleType(leftType, rightType)) {
                addError(expression, "Cannot compare values of types '" + leftType + 
                        "' and '" + rightType + "'");
            }
        } else if (operator.equals("<") || operator.equals(">") || 
                   operator.equals("<=") || operator.equals(">=")) {
            // Comparison operators require numeric types
            if (!isNumericType(leftType)) {
                addError(expression, "Left operand of '" + operator + "' must be numeric");
            }
            if (!isNumericType(rightType)) {
                addError(expression, "Right operand of '" + operator + "' must be numeric");
            }
        }
    }
    
    private void analyzeAssignment(Assignment assignment) {
        String variableName = assignment.getVariable();
        Expression value = assignment.getValue();
        
        // Check if variable exists
        Symbol variable = symbolTable.resolve(variableName);
        if (variable == null) {
            addError(assignment, "Undefined variable '" + variableName + "'");
            return;
        }
        
        // Analyze the value expression
        analyzeExpression(value);
        
        // Check if the types are compatible
        String variableType = variable.getType();
        String valueType = getExpressionType(value);
        if (!variableType.equals(valueType) && !isCompatibleType(variableType, valueType)) {
            addError(assignment, "Cannot assign value of type '" + valueType + 
                    "' to variable of type '" + variableType + "'");
        }
    }
    
    private void analyzeVariable(Variable variable) {
        String name = variable.getName();
        
        // Check if variable exists
        Symbol symbol = symbolTable.resolve(name);
        if (symbol == null) {
            addError(variable, "Undefined variable '" + name + "'");
        }
    }
    
    private void analyzeCall(Call call) {
        String name = call.getName();
        List<Expression> arguments = call.getArguments();
        
        // Check if function exists
        Symbol function = symbolTable.resolve(name);
        if (function == null) {
            addError(call, "Undefined function '" + name + "'");
            return;
        }
        
        // Check that it's a function, not a variable
        if (function.getKind() != SymbolKind.FUNCTION) {
            addError(call, "'" + name + "' is not a function");
            return;
        }
        
        // Analyze each argument
        for (Expression arg : arguments) {
            analyzeExpression(arg);
        }
        
        // Note: Ideally we would also check that the number and types of arguments match
        // the function's parameters, but that would require more complex function type information
    }
    
    private String getExpressionType(Expression expression) {
        if (expression instanceof Literal) {
            return ((Literal) expression).getType();
        } else if (expression instanceof Variable) {
            String name = ((Variable) expression).getName();
            Symbol symbol = symbolTable.resolve(name);
            return symbol != null ? symbol.getType() : "unknown";
        } else if (expression instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expression;
            String operator = binary.getOperator();
            String leftType = getExpressionType(binary.getLeft());
            String rightType = getExpressionType(binary.getRight());
            
            if (operator.equals("+") || operator.equals("-") || 
                operator.equals("*") || operator.equals("/")) {
                // Arithmetic operators
                if (leftType.equals("float") || rightType.equals("float")) {
                    return "float";
                } else {
                    return "int";
                }
            } else if (operator.equals("==") || operator.equals("!=") || 
                       operator.equals("<") || operator.equals(">") || 
                       operator.equals("<=") || operator.equals(">=")) {
                // Comparison operators always return an int (boolean)
                return "int";
            }
            
            return "unknown";
        } else if (expression instanceof Assignment) {
            String variableName = ((Assignment) expression).getVariable();
            Symbol variable = symbolTable.resolve(variableName);
            return variable != null ? variable.getType() : "unknown";
        } else if (expression instanceof Call) {
            String functionName = ((Call) expression).getName();
            Symbol function = symbolTable.resolve(functionName);
            return function != null ? function.getType() : "unknown";
        }
        
        return "unknown";
    }
    
    private boolean isNumericType(String type) {
        return "int".equals(type) || "float".equals(type);
    }
    
    private boolean isCompatibleType(String targetType, String sourceType) {
        // Allow int to be assigned to float
        return "float".equals(targetType) && "int".equals(sourceType);
    }
    
    private void addError(Node node, String message) {
        errors.add("Line " + node.getLine() + ", Column " + node.getColumn() + ": " + message);
    }
}