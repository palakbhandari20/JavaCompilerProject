package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreeAddressCode {
    private List<Function> functions;
    
    public ThreeAddressCode() {
        this.functions = new ArrayList<>();
    }
    
    public void addFunction(Function function) {
        functions.add(function);
    }
    
    public List<Function> getFunctions() {
        return functions;
    }
    
    public static class Function {
        private String name;
        private String returnType;
        private List<String> parameters;
        private List<Instruction> instructions;
        private Map<String, String> variables; // variable name -> type
        private int tempCounter;
        
        public Function(String name, String returnType) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = new ArrayList<>();
            this.instructions = new ArrayList<>();
            this.variables = new HashMap<>();
            this.tempCounter = 0;
        }
        
        public void addParameter(String name, String type) {
            parameters.add(name);
            variables.put(name, type);
        }
        
        public void addVariable(String name, String type) {
            variables.put(name, type);
        }
        
        public void addInstruction(Instruction instruction) {
            instructions.add(instruction);
        }
        
        public String generateTemp(String type) {
            String tempName = "t" + tempCounter++;
            variables.put(tempName, type);
            return tempName;
        }
        
        public String getName() {
            return name;
        }
        
        public String getReturnType() {
            return returnType;
        }
        
        public List<String> getParameters() {
            return parameters;
        }
        
        public List<Instruction> getInstructions() {
            return instructions;
        }
        
        public Map<String, String> getVariables() {
            return variables;
        }
    }
    
    public static abstract class Instruction {
        private String label;
        
        public Instruction() {
            this.label = null;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
        
        public abstract String toString();
    }
    
    public static class BinaryOperation extends Instruction {
        private String result;
        private String left;
        private String operator;
        private String right;
        
        public BinaryOperation(String result, String left, String operator, String right) {
            this.result = result;
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getLeft() {
            return left;
        }
        
        public String getOperator() {
            return operator;
        }
        
        public String getRight() {
            return right;
        }
        
        @Override
        public String toString() {
            return result + " = " + left + " " + operator + " " + right;
        }
    }
    
    public static class UnaryOperation extends Instruction {
        private String result;
        private String operator;
        private String operand;
        
        public UnaryOperation(String result, String operator, String operand) {
            this.result = result;
            this.operator = operator;
            this.operand = operand;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getOperator() {
            return operator;
        }
        
        public String getOperand() {
            return operand;
        }
        
        @Override
        public String toString() {
            return result + " = " + operator + " " + operand;
        }
    }
    
    public static class Copy extends Instruction {
        private String destination;
        private String source;
        
        public Copy(String destination, String source) {
            this.destination = destination;
            this.source = source;
        }
        
        public String getDestination() {
            return destination;
        }
        
        public String getSource() {
            return source;
        }
        
        @Override
        public String toString() {
            return destination + " = " + source;
        }
    }
    
    public static class FunctionCall extends Instruction {
        private String result;
        private String function;
        private List<String> arguments;
        
        public FunctionCall(String result, String function, List<String> arguments) {
            this.result = result;
            this.function = function;
            this.arguments = arguments;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getFunction() {
            return function;
        }
        
        public List<String> getArguments() {
            return arguments;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (result != null) {
                sb.append(result).append(" = ");
            }
            sb.append("call ").append(function).append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments.get(i));
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    public static class Jump extends Instruction {
        private String target;
        
        public Jump(String target) {
            this.target = target;
        }
        
        public String getTarget() {
            return target;
        }
        
        @Override
        public String toString() {
            return "goto " + target;
        }
    }
    
    public static class ConditionalJump extends Instruction {
        private String condition;
        private String trueTarget;
        private String falseTarget;
        
        public ConditionalJump(String condition, String trueTarget, String falseTarget) {
            this.condition = condition;
            this.trueTarget = trueTarget;
            this.falseTarget = falseTarget;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public String getTrueTarget() {
            return trueTarget;
        }
        
        public String getFalseTarget() {
            return falseTarget;
        }
        
        @Override
        public String toString() {
            return "if " + condition + " goto " + trueTarget + " else goto " + falseTarget;
        }
    }
    
    public static class Return extends Instruction {
        private String value;
        
        public Return(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value != null ? "return " + value : "return";
        }
    }
}