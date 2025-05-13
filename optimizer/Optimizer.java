/*
 * File: src/compiler/optimizer/Optimizer.java
 * Performs optimization on the intermediate representation
 */
package optimizer;

import ir.ThreeAddressCode;
import ir.ThreeAddressCode.Function;
import ir.ThreeAddressCode.Instruction;
import ir.ThreeAddressCode.BinaryOperation;
import ir.ThreeAddressCode.Copy;
import ir.ThreeAddressCode.UnaryOperation;
import ir.ThreeAddressCode.FunctionCall;
import ir.ThreeAddressCode.ConditionalJump;
import ir.ThreeAddressCode.Return;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Optimizer {
    public ThreeAddressCode optimize(ThreeAddressCode ir) {
        ThreeAddressCode optimizedIR = new ThreeAddressCode();
        
        // Optimize each function
        for (Function function : ir.getFunctions()) {
            Function optimizedFunction = optimizeFunction(function);
            optimizedIR.addFunction(optimizedFunction);
        }
        
        return optimizedIR;
    }
    
    private Function optimizeFunction(Function function) {
        Function optimizedFunction = new Function(function.getName(), function.getReturnType());
        
        // Copy parameters
        for (String param : function.getParameters()) {
            optimizedFunction.addParameter(param, function.getVariables().get(param));
        }
        
        // Copy variables
        for (Map.Entry<String, String> var : function.getVariables().entrySet()) {
            optimizedFunction.addVariable(var.getKey(), var.getValue());
        }
        
        // Apply optimizations
        List<Instruction> instructions = function.getInstructions();
        List<Instruction> optimizedInstructions = new ArrayList<>();
        
        // Constant folding and propagation
        Map<String, Object> constantValues = new HashMap<>();
        Set<String> modifiedVariables = new HashSet<>();
        
        for (Instruction inst : instructions) {
            Instruction optimizedInst = optimizeInstruction(inst, constantValues, modifiedVariables);
            
            if (optimizedInst != null) {
                optimizedInstructions.add(optimizedInst);
            }
        }
        
        // Dead code elimination
        List<Instruction> eliminatedDeadCode = eliminateDeadCode(optimizedInstructions);
        
        // Add optimized instructions to the function
        for (Instruction inst : eliminatedDeadCode) {
            optimizedFunction.addInstruction(inst);
        }
        
        return optimizedFunction;
    }
    
    private Instruction optimizeInstruction(Instruction inst, 
                                            Map<String, Object> constantValues, 
                                            Set<String> modifiedVariables) {
        if (inst instanceof BinaryOperation) {
            return optimizeBinaryOperation((BinaryOperation) inst, constantValues, modifiedVariables);
        } else if (inst instanceof Copy) {
            return optimizeCopy((Copy) inst, constantValues, modifiedVariables);
        } else if (inst instanceof UnaryOperation) {
            return optimizeUnaryOperation((UnaryOperation) inst, constantValues, modifiedVariables);
        } else {
            // For other instruction types, return as-is
            return inst;
        }
    }
    
    private Instruction optimizeBinaryOperation(BinaryOperation op, 
                                                Map<String, Object> constantValues, 
                                                Set<String> modifiedVariables) {
        String result = op.getResult();
        String left = op.getLeft();
        String right = op.getRight();
        String operator = op.getOperator();
        
        // Check if both operands are constants
        Object leftValue = constantValues.get(left);
        Object rightValue = constantValues.get(right);
        
        if (leftValue != null && rightValue != null) {
            // Perform constant folding
            Object resultValue = performConstantOperation(leftValue, operator, rightValue);
            
            // Record the constant result
            constantValues.put(result, resultValue);
            
            // Create a copy instruction with the constant value
            return new Copy(result, resultValue.toString());
        }
        
        // Remove entries for modified variables
        modifiedVariables.add(result);
        constantValues.remove(result);
        
        return op;
    }
    
    private Instruction optimizeCopy(Copy copy, 
                                     Map<String, Object> constantValues, 
                                     Set<String> modifiedVariables) {
        String dest = copy.getDestination();
        String source = copy.getSource();
        
        // Skip NOP instructions (label placeholders)
        if (dest.equals("nop") && source.equals("nop")) {
            return copy;
        }
        
        // Check if source is a constant
        Object sourceValue = constantValues.get(source);
        if (sourceValue != null) {
            // Propagate constant
            constantValues.put(dest, sourceValue);
            return new Copy(dest, sourceValue.toString());
        }
        
        // Handle regular copy
        if (source.matches("-?\\d+")) {
            // Numeric literal
            constantValues.put(dest, Integer.parseInt(source));
        }
        
        // Remove entries for modified variables
        modifiedVariables.add(dest);
        constantValues.remove(dest);
        
        return copy;
    }
    
    private Instruction optimizeUnaryOperation(UnaryOperation op, 
                                               Map<String, Object> constantValues, 
                                               Set<String> modifiedVariables) {
        String result = op.getResult();
        String operand = op.getOperand();
        String operator = op.getOperator();
        
        // Check if operand is a constant
        Object operandValue = constantValues.get(operand);
        
        if (operandValue != null) {
            // Perform constant folding
            Object resultValue = performConstantUnaryOperation(operator, operandValue);
            
            // Record the constant result
            constantValues.put(result, resultValue);
            
            // Create a copy instruction with the constant value
            return new Copy(result, resultValue.toString());
        }
        
        // Remove entries for modified variables
        modifiedVariables.add(result);
        constantValues.remove(result);
        
        return op;
    }
    
    private Object performConstantOperation(Object left, String operator, Object right) {
        // Assuming left and right are Integer or Float
        if (left instanceof Integer && right instanceof Integer) {
            int leftInt = (Integer) left;
            int rightInt = (Integer) right;
            
            switch (operator) {
                case "+": return leftInt + rightInt;
                case "-": return leftInt - rightInt;
                case "*": return leftInt * rightInt;
                case "/": return leftInt / rightInt;
                case "==": return leftInt == rightInt ? 1 : 0;
                case "!=": return leftInt != rightInt ? 1 : 0;
                case "<": return leftInt < rightInt ? 1 : 0;
                case ">": return leftInt > rightInt ? 1 : 0;
                case "<=": return leftInt <= rightInt ? 1 : 0;
                case ">=": return leftInt >= rightInt ? 1 : 0;
            }
        } else if (left instanceof Float || right instanceof Float) {
            // Convert to float if either operand is float
            float leftFloat = left instanceof Integer ? 
                ((Integer) left).floatValue() : (Float) left;
            float rightFloat = right instanceof Integer ? 
                ((Integer) right).floatValue() : (Float) right;
            
            switch (operator) {
                case "+": return leftFloat + rightFloat;
                case "-": return leftFloat - rightFloat;
                case "*": return leftFloat * rightFloat;
                case "/": return leftFloat / rightFloat;
                case "==": return leftFloat == rightFloat ? 1 : 0;
                case "!=": return leftFloat != rightFloat ? 1 : 0;
                case "<": return leftFloat < rightFloat ? 1 : 0;
                case ">": return leftFloat > rightFloat ? 1 : 0;
                case "<=": return leftFloat <= rightFloat ? 1 : 0;
                case ">=": return leftFloat >= rightFloat ? 1 : 0;
            }
        }
        
        throw new RuntimeException("Unsupported constant operation: " + left + " " + operator + " " + right);
    }
    
    private Object performConstantUnaryOperation(String operator, Object operand) {
        if (operand instanceof Integer) {
            int value = (Integer) operand;
            
            switch (operator) {
                case "-": return -value;
                case "!": return value == 0 ? 1 : 0;
            }
        } else if (operand instanceof Float) {
            float value = (Float) operand;
            
            switch (operator) {
                case "-": return -value;
                case "!": return value == 0.0f ? 1 : 0;
            }
        }
        
        throw new RuntimeException("Unsupported unary operation: " + operator + " " + operand);
    }
    
    private List<Instruction> eliminateDeadCode(List<Instruction> instructions) {
        List<Instruction> filteredInstructions = new ArrayList<>();
        Set<String> usedVariables = new HashSet<>();
        
        // Collect variables from control flow and function call instructions
        for (Instruction inst : instructions) {
            if (inst instanceof ConditionalJump) {
                ConditionalJump jump = (ConditionalJump) inst;
                usedVariables.add(jump.getCondition());
            } else if (inst instanceof FunctionCall) {
                FunctionCall call = (FunctionCall) inst;
                if (call.getResult() != null) {
                    usedVariables.add(call.getResult());
                }
                usedVariables.addAll(call.getArguments());
            } else if (inst instanceof Return) {
                Return ret = (Return) inst;
                if (ret.getValue() != null) {
                    usedVariables.add(ret.getValue());
                }
            }
        }
        
        // Iterate in reverse to track variable uses
        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instruction inst = instructions.get(i);
            
            if (inst instanceof BinaryOperation) {
                BinaryOperation binaryOp = (BinaryOperation) inst;
                String result = binaryOp.getResult();
                String left = binaryOp.getLeft();
                String right = binaryOp.getRight();
                
                if (usedVariables.contains(result)) {
                    usedVariables.add(left);
                    usedVariables.add(right);
                    filteredInstructions.add(0, inst);
                }
            } else if (inst instanceof Copy) {
                Copy copy = (Copy) inst;
                String dest = copy.getDestination();
                String source = copy.getSource();
                
                // Special case for NOP instructions (label placeholders)
                if (dest.equals("nop") && source.equals("nop")) {
                    filteredInstructions.add(0, inst);
                    continue;
                }
                
                if (usedVariables.contains(dest)) {
                    usedVariables.add(source);
                    filteredInstructions.add(0, inst);
                }
            } else {
                // Always keep these types of instructions
                filteredInstructions.add(0, inst);
            }
        }
        
        return filteredInstructions;
    }
}