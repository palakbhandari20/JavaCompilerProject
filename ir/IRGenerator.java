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
    
    public IRGenerator() {
        this.ir = new ThreeAddressCode();
        this.stringLiterals = new HashMap<>();
        this.labelCounter = 0;
    }
    
    public ThreeAddressCode generate(Program program) {
        // Process each function
        for (FunctionDeclaration function : program.getFunctions()) {
            generateFunction(function);
        }
        
        return ir;
    }
    
    private void generateFunction(FunctionDeclaration functionDecl) {
        String name = functionDecl.getName();
        String returnType = functionDecl.getReturnType().getName();
        
        // Create a new function
        currentFunction = new Function(name, returnType);
        
        // Add parameters
        for (Parameter param : functionDecl.getParameters()) {
            currentFunction.addParameter(param.getName(), param.getType().getName());
        }
        
        // Generate code for function body
        generateBlock(functionDecl.getBody());
        
        // Add function to IR
        ir.addFunction(currentFunction);
    }
    
    private void generateBlock(Block block) {
        for (Statement statement : block.getStatements()) {
            generateStatement(statement);
        }
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
        
        // Add variable to function
        currentFunction.addVariable(name, type);
        
        // Generate initialization if present
        if (declaration.getInitialValue() != null) {
            String valueTemp = generateExpression(declaration.getInitialValue());
            currentFunction.addInstruction(new Copy(name, valueTemp));
        }
    }
    
private void generateIfStatement(IfStatement statement) {
        String condition = generateExpression(statement.getCondition());
        String trueLabel = generateLabel();
        String falseLabel = statement.getElseBranch() != null ? generateLabel() : null;
        String endLabel = generateLabel();
        
        // Generate conditional jump
        if (falseLabel != null) {
            currentFunction.addInstruction(new ConditionalJump(condition, trueLabel, falseLabel));
        } else {
            currentFunction.addInstruction(new ConditionalJump(condition, trueLabel, endLabel));
        }
        
        // Generate 'then' branch
        Instruction trueLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        trueLabelInst.setLabel(trueLabel);
        currentFunction.addInstruction(trueLabelInst);
        
        generateStatement(statement.getThenBranch());
        currentFunction.addInstruction(new Jump(endLabel));
        
        // Generate 'else' branch if it exists
        if (statement.getElseBranch() != null) {
            Instruction falseLabelInst = new Copy("nop", "nop"); // Placeholder instruction
            falseLabelInst.setLabel(falseLabel);
            currentFunction.addInstruction(falseLabelInst);
            
            generateStatement(statement.getElseBranch());
            currentFunction.addInstruction(new Jump(endLabel));
        }
        
        // End label
        Instruction endLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        endLabelInst.setLabel(endLabel);
        currentFunction.addInstruction(endLabelInst);
    }
    
    private void generateWhileStatement(WhileStatement statement) {
        String startLabel = generateLabel();
        String bodyLabel = generateLabel();
        String endLabel = generateLabel();
        
        // Start label
        Instruction startLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        startLabelInst.setLabel(startLabel);
        currentFunction.addInstruction(startLabelInst);
        
        // Generate condition and conditional jump
        String condition = generateExpression(statement.getCondition());
        currentFunction.addInstruction(new ConditionalJump(condition, bodyLabel, endLabel));
        
        // Body label
        Instruction bodyLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        bodyLabelInst.setLabel(bodyLabel);
        currentFunction.addInstruction(bodyLabelInst);
        
        // Generate loop body
        generateStatement(statement.getBody());
        
        // Jump back to condition
        currentFunction.addInstruction(new Jump(startLabel));
        
        // End label
        Instruction endLabelInst = new Copy("nop", "nop"); // Placeholder instruction
        endLabelInst.setLabel(endLabel);
        currentFunction.addInstruction(endLabelInst);
    }
    
    private void generateReturnStatement(ReturnStatement statement) {
        if (statement.getValue() != null) {
            String valueTemp = generateExpression(statement.getValue());
            currentFunction.addInstruction(new Return(valueTemp));
        } else {
            currentFunction.addInstruction(new Return(null));
        }
    }
    
    private String generateExpression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            return generateBinaryExpression((BinaryExpression) expression);
        } else if (expression instanceof Assignment) {
            return generateAssignment((Assignment) expression);
        } else if (expression instanceof Variable) {
            return ((Variable) expression).getName();
        } else if (expression instanceof Literal) {
            return generateLiteral((Literal) expression);
        } else if (expression instanceof Call) {
            return generateCall((Call) expression);
        } else {
            throw new RuntimeException("Unsupported expression: " + expression.getClass().getName());
        }
    }
    
    private String generateBinaryExpression(BinaryExpression expression) {
        String left = generateExpression(expression.getLeft());
        String right = generateExpression(expression.getRight());
        String operator = expression.getOperator();
        
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
        currentFunction.addInstruction(new BinaryOperation(result, left, operator, right));
        return result;
    }
    
    private String generateAssignment(Assignment assignment) {
        String variable = assignment.getVariable();
        String value = generateExpression(assignment.getValue());
        
        currentFunction.addInstruction(new Copy(variable, value));
        return variable;
    }
    
    private String generateLiteral(Literal literal) {
        String type = literal.getType();
        Object value = literal.getValue();
        
        if (type.equals("string")) {
            // Handle string literals by assigning them a unique identifier
            String stringId = "str" + stringLiterals.size();
            stringLiterals.put(stringId, (String) value);
            return stringId;
        } else {
            // For numeric literals, create a temporary variable
            String temp = currentFunction.generateTemp(type);
            currentFunction.addInstruction(new Copy(temp, value.toString()));
            return temp;
        }
    }
    
    private String generateCall(Call call) {
        String functionName = call.getName();
        List<String> arguments = new ArrayList<>();
        
        // Generate code for each argument
        for (Expression arg : call.getArguments()) {
            String argTemp = generateExpression(arg);
            arguments.add(argTemp);
        }
        
        // Determine if the function has a return value
        boolean hasReturnValue = !"void".equals(getFunctionReturnType(functionName));
        String result = hasReturnValue ? currentFunction.generateTemp(getFunctionReturnType(functionName)) : null;
        
        currentFunction.addInstruction(new FunctionCall(result, functionName, arguments));
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
        // For simplicity, we'll assume non-void
        return "int";
    }
    
    private String generateLabel() {
        return "L" + labelCounter++;
    }
}