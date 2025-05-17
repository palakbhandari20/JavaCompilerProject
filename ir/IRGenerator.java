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
    
    public IRGenerator() {
        this.ir = new ThreeAddressCode();
        this.stringLiterals = new HashMap<>();
        this.labelCounter = 0;
        this.tempVarCounter = 0;
        System.out.println("=== IR Generation Started ===");
    }
    
    public ThreeAddressCode generate(Program program) {
        System.out.println("\n[IR-GEN] Processing program with " + program.getFunctions().size() + " functions");
        
        // Process each function
        for (FunctionDeclaration function : program.getFunctions()) {
            System.out.println("\n[FUNCTION] Generating IR for function: " + function.getName());
            generateFunction(function);
        }
        
        System.out.println("\n[IR-GEN] IR generation completed");
        System.out.println("[IR-GEN] Generated " + ir.getFunctions().size() + " functions");
        System.out.println("[IR-GEN] Generated " + stringLiterals.size() + " string literals");
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
        
        // Add variable to function
        currentFunction.addVariable(name, type);
        
        // Generate initialization if present
        if (declaration.getInitialValue() != null) {
            System.out.println("        [VAR-INIT] Generating initializer for variable '" + name + "'");
            String valueTemp = generateExpression(declaration.getInitialValue());
            currentFunction.addInstruction(new Copy(name, valueTemp));
            System.out.println("        [INSTR] Added COPY: " + name + " = " + valueTemp);
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
            currentFunction.addInstruction(new Return(valueTemp));
            System.out.println("        [INSTR] Added RETURN with value: " + valueTemp);
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
            System.out.println("          [VAR] Variable reference: " + varName);
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
        
        // Determine the type of the result
        String resultType = "int"; // Default to int
        
        if (operator.equals("+") || operator.equals("-") || 
            operator.equals("*") || operator.equals("/")) {
            // If either operand is float, result is float
            if (getType(left).equals("float") || getType(right).equals("float")) {
                resultType = "float";
            }
        } else if (operator.equals("==") || operator.equals("!=") || 
                  operator.equals("<") || operator.equals(">") || 
                  operator.equals("<=") || operator.equals(">=")) {
            // Comparison operators always return int (boolean)
            resultType = "int";
        }
        
        String result = currentFunction.generateTemp(resultType);
        tempVarCounter++;
        System.out.println("          [TEMP] Created temporary variable: " + result + " of type " + resultType);
        
        currentFunction.addInstruction(new BinaryOperation(result, left, operator, right));
        System.out.println("          [INSTR] Added BINARY_OP: " + result + " = " + left + " " + operator + " " + right);
        
        return result;
    }
    
    private String generateAssignment(Assignment assignment) {
        String variable = assignment.getVariable();
        System.out.println("          [ASSIGN] Generating assignment to variable: " + variable);
        
        System.out.println("          [ASSIGN] Generating value expression");
        String value = generateExpression(assignment.getValue());
        
        currentFunction.addInstruction(new Copy(variable, value));
        System.out.println("          [INSTR] Added COPY: " + variable + " = " + value);
        
        return variable;
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
            String temp = currentFunction.generateTemp(type);
            tempVarCounter++;
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
        boolean hasReturnValue = !"void".equals(getFunctionReturnType(functionName));
        String result = null;
        
        if (hasReturnValue) {
            result = currentFunction.generateTemp(getFunctionReturnType(functionName));
            tempVarCounter++;
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
        // Check if it's a literal
        if (variable.startsWith("str")) {
            return "string";
        } else if (variable.matches("-?\\d+")) {
            return "int";
        } else if (variable.matches("-?\\d+\\.\\d+")) {
            return "float";
        }
        
        // Check in function variables
        if (currentFunction.getVariables().containsKey(variable)) {
            return currentFunction.getVariables().get(variable);
        }
        
        return "unknown";
    }
    
    private String getFunctionReturnType(String functionName) {
        // In a real compiler, this would look up the function's return type in a symbol table
        // For now, we'll assume non-void and log this assumption
        System.out.println("          [TYPE] Assuming return type 'int' for function: " + functionName);
        return "int";
    }
    
    private String generateLabel() {
        String label = "L" + labelCounter++;
        System.out.println("          [LABEL] Generated new label: " + label);
        return label;
    }
}