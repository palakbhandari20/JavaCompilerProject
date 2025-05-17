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
        System.out.println("=== Semantic Analysis Started ===");
    }
    
    public void analyze(Program program) {
        System.out.println("\n[ANALYZER] Starting first pass: declaring functions...");
        // First pass: Declare all functions
        for (FunctionDeclaration function : program.getFunctions()) {
            System.out.println("[FUNCTION-DECLARE] Processing function: " + function.getName());
            declareFunction(function);
        }
        
        System.out.println("\n[ANALYZER] Starting second pass: analyzing function bodies...");
        // Second pass: Check function bodies
        for (FunctionDeclaration function : program.getFunctions()) {
            System.out.println("\n[FUNCTION-ANALYZE] Analyzing function: " + function.getName());
            analyzeFunction(function);
        }
        
        // Report any errors found during analysis
        if (!errors.isEmpty()) {
            System.out.println("\n[ANALYZER] Found " + errors.size() + " semantic errors:");
            StringBuilder errorMessage = new StringBuilder("Semantic errors:\n");
            for (String error : errors) {
                System.out.println("  ERROR: " + error);
                errorMessage.append("- ").append(error).append("\n");
            }
            System.out.println("\n=== Semantic Analysis Failed ===");
            throw new RuntimeException(errorMessage.toString());
        } else {
            System.out.println("\n[ANALYZER] No semantic errors found.");
            System.out.println("=== Semantic Analysis Completed Successfully ===");
        }
    }
    
    private void declareFunction(FunctionDeclaration function) {
        String name = function.getName();
        String returnType = function.getReturnType().getName();
        
        // Check for duplicate function declarations
        Symbol existing = symbolTable.resolve(name);
        if (existing != null) {
            System.out.println("  [ERROR] Function '" + name + "' already declared");
            addError(function, "Function '" + name + "' already declared");
            return;
        }
        
        // Add function to symbol table
        symbolTable.define(new Symbol(name, returnType, SymbolKind.FUNCTION));
        System.out.println("  [OK] Declared function '" + name + "' with return type '" + returnType + "'");
    }
    
    private void analyzeFunction(FunctionDeclaration function) {
        String name = function.getName();
        currentFunction = name;
        
        System.out.println("  [SCOPE] Entering function scope for '" + name + "'");
        // Create a new scope for the function
        symbolTable.enterScope();
        
        // Declare parameters
        System.out.println("  [PARAMS] Processing parameters for function '" + name + "'");
        for (Parameter param : function.getParameters()) {
            String paramName = param.getName();
            String paramType = param.getType().getName();
            
            // Check for duplicate parameter names
            Symbol existing = symbolTable.resolve(paramName);
            if (existing != null && symbolTable.isInGlobalScope()) {
                System.out.println("    [ERROR] Parameter '" + paramName + "' already declared");
                addError(param, "Parameter '" + paramName + "' already declared");
                continue;
            }
            
            symbolTable.define(new Symbol(paramName, paramType, SymbolKind.PARAMETER));
            System.out.println("    [OK] Declared parameter '" + paramName + "' with type '" + paramType + "'");
        }
        
        // Analyze function body
        System.out.println("  [BODY] Analyzing body of function '" + name + "'");
        analyzeBlock(function.getBody());
        
        // Exit function scope
        System.out.println("  [SCOPE] Exiting function scope for '" + name + "'");
        symbolTable.exitScope();
        currentFunction = null;
    }
    
    private void analyzeBlock(Block block) {
        // Create a new scope for the block
        System.out.println("    [SCOPE] Entering new block scope");
        symbolTable.enterScope();
        
        // Analyze each statement in the block
        int statementCount = block.getStatements().size();
        System.out.println("    [BLOCK] Processing " + statementCount + " statements");
        
        for (int i = 0; i < statementCount; i++) {
            Statement statement = block.getStatements().get(i);
            System.out.println("      [STMT-" + (i+1) + "/" + statementCount + "] Processing " + 
                              getStatementType(statement));
            analyzeStatement(statement);
        }
        
        // Exit block scope
        System.out.println("    [SCOPE] Exiting block scope");
        symbolTable.exitScope();
    }
    
    private String getStatementType(Statement statement) {
        if (statement instanceof ExpressionStatement) return "expression statement";
        else if (statement instanceof IfStatement) return "if statement";
        else if (statement instanceof WhileStatement) return "while statement";
        else if (statement instanceof ReturnStatement) return "return statement";
        else if (statement instanceof VarDeclarationStatement) return "variable declaration";
        else if (statement instanceof Block) return "block";
        else return "unknown statement";
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
        System.out.println("        [IF] Analyzing condition");
        Expression condition = statement.getCondition();
        analyzeExpression(condition);
        
        // Analyze the 'then' branch
        System.out.println("        [IF] Analyzing 'then' branch");
        analyzeStatement(statement.getThenBranch());
        
        // Analyze the 'else' branch if it exists
        if (statement.getElseBranch() != null) {
            System.out.println("        [IF] Analyzing 'else' branch");
            analyzeStatement(statement.getElseBranch());
        }
    }
    
    private void analyzeWhileStatement(WhileStatement statement) {
        System.out.println("        [WHILE] Analyzing condition");
        Expression condition = statement.getCondition();
        analyzeExpression(condition);
        
        // Analyze the loop body
        System.out.println("        [WHILE] Analyzing loop body");
        analyzeStatement(statement.getBody());
    }
    
    private void analyzeReturnStatement(ReturnStatement statement) {
        System.out.println("        [RETURN] Analyzing return statement");
        // Check if we're in a function
        if (currentFunction == null) {
            System.out.println("          [ERROR] Return statement outside of function");
            addError(statement, "Return statement outside of function");
            return;
        }
        
        // Get the function's return type
        Symbol function = symbolTable.resolve(currentFunction);
        if (function == null) {
            System.out.println("          [ERROR] Unknown function '" + currentFunction + "'");
            addError(statement, "Unknown function '" + currentFunction + "'");
            return;
        }
        
        String returnType = function.getType();
        Expression value = statement.getValue();
        
        // Check if the return type matches
        if ("void".equals(returnType) && value != null) {
            System.out.println("          [ERROR] Cannot return a value from a void function");
            addError(statement, "Cannot return a value from a void function");
        } else if (!"void".equals(returnType) && value == null) {
            System.out.println("          [ERROR] Function must return a value of type '" + returnType + "'");
            addError(statement, "Function must return a value of type '" + returnType + "'");
        } else if (value != null) {
            System.out.println("          [RETURN] Analyzing return value");
            analyzeExpression(value);
            String valueType = getExpressionType(value);
            if (!returnType.equals(valueType) && !isCompatibleType(returnType, valueType)) {
                System.out.println("          [ERROR] Return type mismatch: expected '" + returnType + 
                        "', got '" + valueType + "'");
                addError(statement, "Return type mismatch: expected '" + returnType + 
                        "', got '" + valueType + "'");
            } else {
                System.out.println("          [OK] Return value type '" + valueType + 
                        "' matches function return type '" + returnType + "'");
            }
        }
    }
    
    private void analyzeVarDeclaration(VarDeclaration declaration) {
        String name = declaration.getName();
        String type = declaration.getType().getName();
        
        System.out.println("        [VAR] Analyzing variable declaration '" + name + "' of type '" + type + "'");
        
        // Check for duplicate variable declarations in the same scope
        Symbol existing = symbolTable.resolve(name);
        if (existing != null && !symbolTable.isInGlobalScope()) {
            System.out.println("          [ERROR] Variable '" + name + "' already declared");
            addError(declaration, "Variable '" + name + "' already declared");
            return;
        }
        
        // Check the initializer if present
        Expression initialValue = declaration.getInitialValue();
        if (initialValue != null) {
            System.out.println("          [VAR] Analyzing initializer for variable '" + name + "'");
            analyzeExpression(initialValue);
            String valueType = getExpressionType(initialValue);
            if (!type.equals(valueType) && !isCompatibleType(type, valueType)) {
                System.out.println("          [ERROR] Cannot initialize variable of type '" + type + 
                        "' with value of type '" + valueType + "'");
                addError(declaration, "Cannot initialize variable of type '" + type + 
                        "' with value of type '" + valueType + "'");
            } else {
                System.out.println("          [OK] Initializer type '" + valueType + "' is compatible with variable type '" + type + "'");
            }
        }
        
        // Add variable to symbol table
        symbolTable.define(new Symbol(name, type, SymbolKind.VARIABLE));
        System.out.println("          [OK] Declared variable '" + name + "' with type '" + type + "'");
    }
    
    private void analyzeExpression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            System.out.println("          [EXPR] Analyzing binary expression");
            analyzeBinaryExpression((BinaryExpression) expression);
        } else if (expression instanceof Assignment) {
            String varName = ((Assignment) expression).getVariable();
            System.out.println("          [EXPR] Analyzing assignment to '" + varName + "'");
            analyzeAssignment((Assignment) expression);
        } else if (expression instanceof Variable) {
            String varName = ((Variable) expression).getName();
            System.out.println("          [EXPR] Analyzing variable reference '" + varName + "'");
            analyzeVariable((Variable) expression);
        } else if (expression instanceof Call) {
            String funcName = ((Call) expression).getName();
            System.out.println("          [EXPR] Analyzing function call to '" + funcName + "'");
            analyzeCall((Call) expression);
        } else if (expression instanceof Literal) {
            Object value = ((Literal) expression).getValue();
            String type = ((Literal) expression).getType();
            System.out.println("          [EXPR] Found literal of type '" + type + "': " + value);
            // No analysis needed for literals
        }
    }
    
    private void analyzeBinaryExpression(BinaryExpression expression) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();
        String operator = expression.getOperator();
        
        System.out.println("            [BINARY] Analyzing left operand of '" + operator + "' operator");
        analyzeExpression(left);
        System.out.println("            [BINARY] Analyzing right operand of '" + operator + "' operator");
        analyzeExpression(right);
        
        String leftType = getExpressionType(left);
        String rightType = getExpressionType(right);
        
        System.out.println("            [BINARY] Checking compatibility of types: left='" + leftType + 
                          "', right='" + rightType + "', operator='" + operator + "'");
        
        // Check if the operator is valid for the types
        if (operator.equals("+") || operator.equals("-") || 
            operator.equals("*") || operator.equals("/")) {
            // Arithmetic operators require numeric types
            if (!isNumericType(leftType)) {
                System.out.println("            [ERROR] Left operand of '" + operator + "' must be numeric");
                addError(expression, "Left operand of '" + operator + "' must be numeric");
            }
            if (!isNumericType(rightType)) {
                System.out.println("            [ERROR] Right operand of '" + operator + "' must be numeric");
                addError(expression, "Right operand of '" + operator + "' must be numeric");
            }
        } else if (operator.equals("==") || operator.equals("!=")) {
            // Equality operators require compatible types
            if (!leftType.equals(rightType) && !isCompatibleType(leftType, rightType)) {
                System.out.println("            [ERROR] Cannot compare values of types '" + leftType + 
                        "' and '" + rightType + "'");
                addError(expression, "Cannot compare values of types '" + leftType + 
                        "' and '" + rightType + "'");
            }
        } else if (operator.equals("<") || operator.equals(">") || 
                   operator.equals("<=") || operator.equals(">=")) {
            // Comparison operators require numeric types
            if (!isNumericType(leftType)) {
                System.out.println("            [ERROR] Left operand of '" + operator + "' must be numeric");
                addError(expression, "Left operand of '" + operator + "' must be numeric");
            }
            if (!isNumericType(rightType)) {
                System.out.println("            [ERROR] Right operand of '" + operator + "' must be numeric");
                addError(expression, "Right operand of '" + operator + "' must be numeric");
            }
        }
        
        if (errors.isEmpty()) {
            System.out.println("            [OK] Binary expression is valid");
        }
    }
    
    private void analyzeAssignment(Assignment assignment) {
        String variableName = assignment.getVariable();
        Expression value = assignment.getValue();
        
        // Check if variable exists
        Symbol variable = symbolTable.resolve(variableName);
        if (variable == null) {
            System.out.println("            [ERROR] Undefined variable '" + variableName + "'");
            addError(assignment, "Undefined variable '" + variableName + "'");
            return;
        } else {
            System.out.println("            [OK] Found variable '" + variableName + "' of type '" + 
                              variable.getType() + "'");
        }
        
        // Analyze the value expression
        System.out.println("            [ASSIGN] Analyzing value to assign to '" + variableName + "'");
        analyzeExpression(value);
        
        // Check if the types are compatible
        String variableType = variable.getType();
        String valueType = getExpressionType(value);
        if (!variableType.equals(valueType) && !isCompatibleType(variableType, valueType)) {
            System.out.println("            [ERROR] Cannot assign value of type '" + valueType + 
                    "' to variable of type '" + variableType + "'");
            addError(assignment, "Cannot assign value of type '" + valueType + 
                    "' to variable of type '" + variableType + "'");
        } else {
            System.out.println("            [OK] Assignment type check passed");
        }
    }
    
    private void analyzeVariable(Variable variable) {
        String name = variable.getName();
        
        // Check if variable exists
        Symbol symbol = symbolTable.resolve(name);
        if (symbol == null) {
            System.out.println("            [ERROR] Undefined variable '" + name + "'");
            addError(variable, "Undefined variable '" + name + "'");
        } else {
            System.out.println("            [OK] Found variable '" + name + "' of type '" + 
                              symbol.getType() + "'");
        }
    }
    
    private void analyzeCall(Call call) {
        String name = call.getName();
        List<Expression> arguments = call.getArguments();
        
        // Check if function exists
        Symbol function = symbolTable.resolve(name);
        if (function == null) {
            System.out.println("            [ERROR] Undefined function '" + name + "'");
            addError(call, "Undefined function '" + name + "'");
            return;
        }
        
        // Check that it's a function, not a variable
        if (function.getKind() != SymbolKind.FUNCTION) {
            System.out.println("            [ERROR] '" + name + "' is not a function");
            addError(call, "'" + name + "' is not a function");
            return;
        }
        
        System.out.println("            [CALL] Function '" + name + "' found with return type '" + 
                          function.getType() + "'");
        
        // Analyze each argument
        System.out.println("            [CALL] Analyzing " + arguments.size() + " arguments");
        for (int i = 0; i < arguments.size(); i++) {
            System.out.println("              [ARG-" + (i+1) + "] Analyzing argument");
            analyzeExpression(arguments.get(i));
        }
        
        System.out.println("            [OK] Function call is valid");
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