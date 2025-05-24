package ir;

import parser.AST.*;
import ir.ThreeAddressCode.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRGenerator {
    private ThreeAddressCode ir;
    private Function currentFunction;
    private Map<String, String> stringLiterals;
    private int labelCounter;
    private int tempVarCounter; // For tracking temporary variables
    private Map<String, String> symbolTable; // Track variable types across functions
    
    public IRGenerator() {
        this.ir = new ThreeAddressCode();
        this.stringLiterals = new HashMap<>();
        this.symbolTable = new HashMap<>();
        this.labelCounter = 0;
        this.tempVarCounter = 0;
        System.out.println("=== IR Generation Started ===");
    }
    
    public ThreeAddressCode generate(Program program) {
        System.out.println("\n[IR-GEN] Processing program with " + program.getFunctions().size() + " functions");
        
        // First pass: collect function signatures for proper type resolution
        for (FunctionDeclaration function : program.getFunctions()) {
            String funcName = function.getName();
            String returnType = function.getReturnType().getName();
            symbolTable.put("func_" + funcName, returnType);
            System.out.println("[SYMBOL] Registered function: " + funcName + " -> " + returnType);
        }
        
        // Process each function
        for (FunctionDeclaration function : program.getFunctions()) {
            System.out.println("\n[FUNCTION] Generating IR for function: " + function.getName());
            generateFunction(function);
        }
        
        System.out.println("\n[IR-GEN] IR generation completed");
        System.out.println("[IR-GEN] Generated " + ir.getFunctions().size() + " functions");
        System.out.println("[IR-GEN] Generated " + stringLiterals.size() + " string literals");
        System.out.println("[IR-GEN] Generated " + tempVarCounter + " temporary variables");
        System.out.println("=== IR Generation Completed Successfully ===");
        
        return ir;
    }
    
    private void generateFunction(FunctionDeclaration functionDecl) {
        String name = functionDecl.getName();
        String returnType = functionDecl.getReturnType().getName();
        
        // Create a new function
        currentFunction = new Function(name, returnType);
        System.out.println("  [FUNCTION] Created function '" + name + "' with return type '" + returnType + "'");
        
        // Add parameters
        System.out.println("  [PARAMS] Processing " + functionDecl.getParameters().size() + " parameters");
        for (Parameter param : functionDecl.getParameters()) {
            String paramName = param.getName();
            String paramType = param.getType().getName();
            currentFunction.addParameter(paramName, paramType);
            // Also add to local symbol table for type tracking
            symbolTable.put(name + "_" + paramName, paramType);
            System.out.println("    [PARAM] Added parameter '" + paramName + "' of type '" + paramType + "'");
        }
        
        // Generate code for function body
        System.out.println("  [BODY] Generating code for function body");
        generateBlock(functionDecl.getBody());
        
        // Add function to IR
        ir.addFunction(currentFunction);
        System.out.println("  [COMPLETE] Function '" + name + "' IR generation completed with " + 
                          currentFunction.getInstructions().size() + " instructions");
    }
    
    private void generateBlock(Block block) {
        System.out.println("    [BLOCK] Processing block with " + block.getStatements().size() + " statements");
        for (Statement statement : block.getStatements()) {
            System.out.println("      [STMT] Processing " + getStatementType(statement));
            generateStatement(statement);
        }
    }
    
    private String getStatementType(Statement statement) {
        if (statement instanceof ExpressionStatement) return "expression statement";
        else if (statement instanceof IfStatement) return "if statement";
        else if (statement instanceof WhileStatement) return "while statement";
        else if (statement instanceof ReturnStatement) return "return statement";
        else if (statement instanceof VarDeclarationStatement) return "variable declaration";
        else if (statement instanceof Block) return "block";
        else return "unknown statement type";
    }
    
    private void generateStatement(Statement statement) {
        if (statement instanceof ExpressionStatement) {
            generateExpression(((ExpressionStatement) statement).getExpression());
        } else if (statement instanceof VarDeclarationStatement) {
            generateVarDeclaration(((VarDeclarationStatement) statement).getDeclaration());
        } else if (statement instanceof IfStatement) {
            generateIfStatement((IfStatement) statement);
        } else if (statement instanceof WhileStatement) {
            generateWhileStatement((WhileStatement) statement);
        } else if (statement instanceof ReturnStatement) {
            generateReturnStatement((ReturnStatement) statement);
        } else if (statement instanceof Block) {
            generateBlock((Block) statement);
        }
    }
    
    private void generateVarDeclaration(VarDeclaration declaration) {
        String name = declaration.getName();
        String type = declaration.getType().getName();
        
        System.out.println("        [VAR-DECL] Declaring variable '" + name + "' of type '" + type + "'");
        
        // Add variable to function and symbol table
        currentFunction.addVariable(name, type);
        symbolTable.put(currentFunction.getName() + "_" + name, type);
        
        // Generate initialization if present
        if (declaration.getInitialValue() != null) {
            System.out.println("        [VAR-INIT] Generating initializer for variable '" + name + "'");
            String valueTemp = generateExpression(declaration.getInitialValue());
            
            // Ensure proper type conversion if needed
            if (!getType(valueTemp).equals(type) && !valueTemp.equals("void")) {
                String convertedTemp = generateTypeConversion(valueTemp, getType(valueTemp), type);
                currentFunction.addInstruction(new Copy(name, convertedTemp));
                System.out.println("        [INSTR] Added COPY with conversion: " + name + " = " + convertedTemp);
            } else {
                currentFunction.addInstruction(new Copy(name, valueTemp));
                System.out.println("        [INSTR] Added COPY: " + name + " = " + valueTemp);
            }
        } else {
            // Initialize with default value
            String defaultValue = getDefaultValue(type);
            currentFunction.addInstruction(new Copy(name, defaultValue));
            System.out.println("        [INSTR] Added default initialization: " + name + " = " + defaultValue);
        }
    }
    
    private void generateIfStatement(IfStatement statement) {
        System.out.println("        [IF] Generating condition for if statement");
        String condition = generateExpression(statement.getCondition());
        String trueLabel = generateLabel();
        String falseLabel = statement.getElseBranch() != null ? generateLabel() : null;
        String endLabel = generateLabel();
        
        System.out.println("        [IF] Created labels - true: " + trueLabel + 
                          (falseLabel != null ? ", false: " + falseLabel : "") + 
                          ", end: " + endLabel);
        
        // Generate conditional jump
        if (falseLabel != null) {
            currentFunction.addInstruction(new ConditionalJump(condition, trueLabel, falseLabel));
            System.out.println("        [INSTR] Added COND_JUMP: if " + condition + " goto " + 
                              trueLabel + " else goto " + falseLabel);
        } else {
            currentFunction.addInstruction(new ConditionalJump(condition, trueLabel, endLabel));
            System.out.println("        [INSTR] Added COND_JUMP: if " + condition + " goto " + 
                              trueLabel + " else goto " + endLabel);
        }
        
        // Generate 'then' branch
        Instruction trueLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        trueLabelInst.setLabel(trueLabel);
        currentFunction.addInstruction(trueLabelInst);
        System.out.println("        [LABEL] Added label: " + trueLabel);
        
        System.out.println("        [IF-THEN] Generating 'then' branch code");
        generateStatement(statement.getThenBranch());
        
        currentFunction.addInstruction(new Jump(endLabel));
        System.out.println("        [INSTR] Added JUMP to: " + endLabel);
        
        // Generate 'else' branch if it exists
        if (statement.getElseBranch() != null) {
            Instruction falseLabelInst = new Copy("nop", "nop"); // Placeholder instruction
            falseLabelInst.setLabel(falseLabel);
            currentFunction.addInstruction(falseLabelInst);
            System.out.println("        [LABEL] Added label: " + falseLabel);
            
            System.out.println("        [IF-ELSE] Generating 'else' branch code");
            generateStatement(statement.getElseBranch());
            
            currentFunction.addInstruction(new Jump(endLabel));
            System.out.println("        [INSTR] Added JUMP to: " + endLabel);
        }
        
        // End label
        Instruction endLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        endLabelInst.setLabel(endLabel);
        currentFunction.addInstruction(endLabelInst);
        System.out.println("        [LABEL] Added label: " + endLabel);
    }
    
    private void generateWhileStatement(WhileStatement statement) {
        String startLabel = generateLabel();
        String bodyLabel = generateLabel();
        String endLabel = generateLabel();
        
        System.out.println("        [WHILE] Created labels - start: " + startLabel + 
                          ", body: " + bodyLabel + ", end: " + endLabel);
        
        // Start label
        Instruction startLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        startLabelInst.setLabel(startLabel);
        currentFunction.addInstruction(startLabelInst);
        System.out.println("        [LABEL] Added label: " + startLabel);
        
        // Generate condition and conditional jump
        System.out.println("        [WHILE] Generating condition expression");
        String condition = generateExpression(statement.getCondition());
        
        currentFunction.addInstruction(new ConditionalJump(condition, bodyLabel, endLabel));
        System.out.println("        [INSTR] Added COND_JUMP: if " + condition + " goto " + 
                          bodyLabel + " else goto " + endLabel);
        
        // Body label
        Instruction bodyLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        bodyLabelInst.setLabel(bodyLabel);
        currentFunction.addInstruction(bodyLabelInst);
        System.out.println("        [LABEL] Added label: " + bodyLabel);
        
        // Generate loop body
        System.out.println("        [WHILE-BODY] Generating loop body code");
        generateStatement(statement.getBody());
        
        // Jump back to condition
        currentFunction.addInstruction(new Jump(startLabel));
        System.out.println("        [INSTR] Added JUMP back to condition: " + startLabel);
        
        // End label
        Instruction endLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        endLabelInst.setLabel(endLabel);
        currentFunction.addInstruction(endLabelInst);
        System.out.println("        [LABEL] Added label: " + endLabel);
    }
    
    private void generateReturnStatement(ReturnStatement statement) {
        System.out.println("        [RETURN] Generating return statement");
        
        if (statement.getValue() != null) {
            System.out.println("        [RETURN] Return has value expression");
            String valueTemp = generateExpression(statement.getValue());
            
            // Ensure return value matches function return type
            String expectedType = currentFunction.getReturnType();
            String actualType = getType(valueTemp);
            
            if (!actualType.equals(expectedType) && !valueTemp.equals("void")) {
                String convertedTemp = generateTypeConversion(valueTemp, actualType, expectedType);
                currentFunction.addInstruction(new Return(convertedTemp));
                System.out.println("        [INSTR] Added RETURN with converted value: " + convertedTemp);
            } else {
                currentFunction.addInstruction(new Return(valueTemp));
                System.out.println("        [INSTR] Added RETURN with value: " + valueTemp);
            }
        } else {
            currentFunction.addInstruction(new Return(null));
            System.out.println("        [INSTR] Added RETURN with no value (void)");
        }
    }
    
    private String generateExpression(Expression expression) {
        System.out.println("          [EXPR] Generating expression of type: " + expression.getClass().getSimpleName());
        
        if (expression instanceof BinaryExpression) {
            return generateBinaryExpression((BinaryExpression) expression);
        } else if (expression instanceof Assignment) {
            return generateAssignment((Assignment) expression);
        } else if (expression instanceof Variable) {
            String varName = ((Variable) expression).getName();
            System.out.println("          [VAR] Variable reference: " + varName + " (type: " + getType(varName) + ")");
            return varName;
        } else if (expression instanceof Literal) {
            return generateLiteral((Literal) expression);
        } else if (expression instanceof Call) {
            return generateCall((Call) expression);
        } else {
            System.out.println("          [ERROR] Unsupported expression: " + expression.getClass().getName());
            throw new RuntimeException("Unsupported expression: " + expression.getClass().getName());
        }
    }
    
    private String generateBinaryExpression(BinaryExpression expression) {
        String operator = expression.getOperator();
        System.out.println("          [BINARY] Generating binary expression with operator: " + operator);
        
        System.out.println("          [BINARY] Generating left operand");
        String left = generateExpression(expression.getLeft());
        
        System.out.println("          [BINARY] Generating right operand");
        String right = generateExpression(expression.getRight());
        
        // Get operand types for proper result type determination
        String leftType = getType(left);
        String rightType = getType(right);
        System.out.println("          [BINARY] Operand types - left: " + leftType + ", right: " + rightType);
        
        // Determine the type of the result
        String resultType = determineResultType(operator, leftType, rightType);
        System.out.println("          [BINARY] Result type determined as: " + resultType);
        
        // Generate type conversions if needed
        if (!leftType.equals(resultType) && isArithmeticOperator(operator)) {
            left = generateTypeConversion(left, leftType, resultType);
            System.out.println("          [BINARY] Converted left operand to " + resultType + ": " + left);
        }
        if (!rightType.equals(resultType) && isArithmeticOperator(operator)) {
            right = generateTypeConversion(right, rightType, resultType);
            System.out.println("          [BINARY] Converted right operand to " + resultType + ": " + right);
        }
        
        String result = generateTempVar(resultType);
        System.out.println("          [TEMP] Created temporary variable for sum/operation: " + result + " of type " + resultType);
        
        currentFunction.addInstruction(new BinaryOperation(result, left, operator, right));
        System.out.println("          [INSTR] Added BINARY_OP (SUM COMPUTATION): " + result + " = " + left + " " + operator + " " + right);
        
        // Special logging for sum operations
        if (operator.equals("+")) {
            System.out.println("          [SUM] *** SUM COMPUTATION GENERATED *** " + result + " = " + left + " + " + right);
        }
        
        return result;
    }
    
    private String generateAssignment(Assignment assignment) {
        String variable = assignment.getVariable();
        System.out.println("          [ASSIGN] Generating assignment to variable: " + variable);
        
        System.out.println("          [ASSIGN] Generating value expression");
        String value = generateExpression(assignment.getValue());
        
        // Type checking and conversion
        String varType = getType(variable);
        String valueType = getType(value);
        
        if (!varType.equals(valueType) && !value.equals("void")) {
            String convertedValue = generateTypeConversion(value, valueType, varType);
            currentFunction.addInstruction(new Copy(variable, convertedValue));
            System.out.println("          [INSTR] Added COPY with conversion: " + variable + " = " + convertedValue);
            return variable;
        } else {
            currentFunction.addInstruction(new Copy(variable, value));
            System.out.println("          [INSTR] Added COPY: " + variable + " = " + value);
            return variable;
        }
    }
    
    private String generateLiteral(Literal literal) {
        String type = literal.getType();
        Object value = literal.getValue();
        
        System.out.println("          [LITERAL] Processing literal of type '" + type + "' with value: " + value);
        
        if (type.equals("string")) {
            // Handle string literals by assigning them a unique identifier
            String stringId = "str" + stringLiterals.size();
            stringLiterals.put(stringId, (String) value);
            System.out.println("          [STRING] Created string literal: " + stringId + " = \"" + value + "\"");
            return stringId;
        } else {
            // For numeric literals, create a temporary variable
            String temp = generateTempVar(type);
            currentFunction.addInstruction(new Copy(temp, value.toString()));
            System.out.println("          [LITERAL] Created temporary for " + type + " literal: " + temp + " = " + value);
            return temp;
        }
    }
    
    private String generateCall(Call call) {
        String functionName = call.getName();
        List<String> arguments = new ArrayList<>();
        
        System.out.println("          [CALL] Generating call to function: " + functionName);
        System.out.println("          [CALL] Processing " + call.getArguments().size() + " arguments");
        
        // Generate code for each argument
        for (int i = 0; i < call.getArguments().size(); i++) {
            System.out.println("          [ARG-" + (i+1) + "] Generating argument expression");
            String argTemp = generateExpression(call.getArguments().get(i));
            arguments.add(argTemp);
            System.out.println("          [ARG-" + (i+1) + "] Argument value: " + argTemp);
        }
        
        // Determine if the function has a return value
        String returnType = getFunctionReturnType(functionName);
        boolean hasReturnValue = !"void".equals(returnType);
        String result = null;
        
        if (hasReturnValue) {
            result = generateTempVar(returnType);
            System.out.println("          [CALL] Function returns value, created temporary: " + result);
        } else {
            System.out.println("          [CALL] Function does not return a value (void)");
        }
        
        currentFunction.addInstruction(new FunctionCall(result, functionName, arguments));
        System.out.println("          [INSTR] Added CALL: " + 
                          (result != null ? result + " = " : "") + 
                          functionName + "(" + String.join(", ", arguments) + ")");
        
        return result != null ? result : "void";
    }
    
    private String getType(String variable) {
        // Check if it's a temporary variable
        if (variable.startsWith("t") && variable.matches("t\\d+")) {
            // Look up in function's temporary variable registry
            if (currentFunction.getVariables().containsKey(variable)) {
                return currentFunction.getVariables().get(variable);
            }
        }
        
        // Check if it's a string literal
        if (variable.startsWith("str")) {
            return "string";
        } 
        
        // Check if it's a numeric literal
        if (variable.matches("-?\\d+")) {
            return "int";
        } else if (variable.matches("-?\\d+\\.\\d+")) {
            return "float";
        }
        
        // Check in local function variables
        if (currentFunction.getVariables().containsKey(variable)) {
            return currentFunction.getVariables().get(variable);
        }
        
        // Check in global symbol table
        String funcVarKey = currentFunction.getName() + "_" + variable;
        if (symbolTable.containsKey(funcVarKey)) {
            return symbolTable.get(funcVarKey);
        }
        
        System.out.println("          [WARNING] Unknown type for variable: " + variable + ", assuming int");
        return "int"; // Default assumption
    }
    
    private String getFunctionReturnType(String functionName) {
        String key = "func_" + functionName;
        if (symbolTable.containsKey(key)) {
            return symbolTable.get(key);
        }
        
        // Special handling for built-in functions
        if (functionName.equals("printf") || functionName.equals("println")) {
            return "void";
        }
        
        System.out.println("          [TYPE] Assuming return type 'int' for function: " + functionName);
        return "int";
    }
    
    private String generateLabel() {
        String label = "L" + labelCounter++;
        System.out.println("          [LABEL] Generated new label: " + label);
        return label;
    }
    
    private String generateTempVar(String type) {
        String temp = currentFunction.generateTemp(type);
        tempVarCounter++;
        System.out.println("          [TEMP-VAR] Generated temporary variable: " + temp + " of type " + type);
        return temp;
    }
    
    private String determineResultType(String operator, String leftType, String rightType) {
        if (operator.equals("+") || operator.equals("-") || 
            operator.equals("*") || operator.equals("/")) {
            // Arithmetic operators: if either operand is float, result is float
            if (leftType.equals("float") || rightType.equals("float")) {
                return "float";
            }
            return "int";
        } else if (operator.equals("==") || operator.equals("!=") || 
                  operator.equals("<") || operator.equals(">") || 
                  operator.equals("<=") || operator.equals(">=")) {
            // Comparison operators always return boolean (represented as int)
            return "int";
        } else if (operator.equals("&&") || operator.equals("||")) {
            // Logical operators return boolean
            return "int";
        }
        
        return "int"; // Default
    }
    
    private boolean isArithmeticOperator(String operator) {
        return operator.equals("+") || operator.equals("-") || 
               operator.equals("*") || operator.equals("/");
    }
    
    private String generateTypeConversion(String variable, String fromType, String toType) {
        if (fromType.equals(toType)) {
            return variable; // No conversion needed
        }
        
        String temp = generateTempVar(toType);
        // Add a conversion instruction (this would be handled by the backend)
        currentFunction.addInstruction(new Copy(temp, "convert(" + variable + ", " + toType + ")"));
        System.out.println("          [CONVERT] Added type conversion: " + temp + " = convert(" + variable + ", " + toType + ")");
        
        return temp;
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "int":
                return "0";
            case "float":
                return "0.0";
            case "string":
                return "\"\"";
            default:
                return "0";
        }
    }
}