package codegen;

import ir.ThreeAddressCode;
import ir.ThreeAddressCode.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CodeGenerator {
    private StringBuilder assembly;
    private Map<String, Integer> variableOffsets;
    private Map<String, String> stringLiterals;
    private int stackOffset;
    private int labelCounter;
    private Set<String> usedLabels;
    
    public CodeGenerator() {
        this.assembly = new StringBuilder();
        this.variableOffsets = new HashMap<>();
        this.stringLiterals = new HashMap<>();
        this.stackOffset = 0;
        this.labelCounter = 0;
        this.usedLabels = new HashSet<>();
        System.out.println("=== Code Generation Started ===");
    }
    
    public void generateCode(ThreeAddressCode ir, String outputFile) {
        System.out.println("\n[CODEGEN] Starting code generation for " + ir.getFunctions().size() + " functions");
        
        // Generate assembly header
        generateHeader();
        
        // Generate string literals section
        generateDataSection();
        
        // Generate code for each function
        for (Function function : ir.getFunctions()) {
            System.out.println("\n[FUNCTION] Generating assembly for function: " + function.getName());
            generateFunction(function);
        }
        
        // Write to file
        try {
            writeToFile(outputFile);
            System.out.println("\n[OUTPUT] Assembly code written to: " + outputFile);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write assembly file: " + e.getMessage());
        }
        
        System.out.println("=== Code Generation Completed ===");
    }
    
    private void generateHeader() {
        assembly.append("; Generated Assembly Code\n");
        assembly.append(".intel_syntax noprefix\n");
        assembly.append(".text\n\n");
    }
    
    private void generateDataSection() {
        if (!stringLiterals.isEmpty()) {
            assembly.append(".section .rodata\n");
            for (Map.Entry<String, String> entry : stringLiterals.entrySet()) {
                assembly.append(entry.getKey()).append(": .asciz \"")
                        .append(entry.getValue()).append("\"\n");
            }
            assembly.append("\n.text\n");
        }
    }
    
    private void generateFunction(Function function) {
        String funcName = function.getName();
        
        // Reset per-function state
        variableOffsets.clear();
        stackOffset = 0;
        
        // Function label and prologue
        assembly.append(".globl ").append(funcName).append("\n");
        assembly.append(funcName).append(":\n");
        assembly.append("    push rbp\n");
        assembly.append("    mov rbp, rsp\n");
        
        System.out.println("  [PROLOGUE] Generated function prologue for: " + funcName);
        
        // Allocate space for local variables
        allocateVariables(function);
        
        // Generate code for each instruction
        System.out.println("  [INSTRUCTIONS] Processing " + function.getInstructions().size() + " instructions");
        for (Instruction instruction : function.getInstructions()) {
            generateInstruction(instruction);
        }
        
        // Function epilogue (if no explicit return was generated)
        assembly.append("    mov rsp, rbp\n");
        assembly.append("    pop rbp\n");
        assembly.append("    ret\n\n");
        
        System.out.println("  [EPILOGUE] Generated function epilogue for: " + funcName);
    }
    
    private void allocateVariables(Function function) {
        Map<String, String> variables = function.getVariables();
        System.out.println("    [VARS] Allocating space for " + variables.size() + " variables");
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String varName = entry.getKey();
            String varType = entry.getValue();
            
            // Skip parameters (they're already on the stack via calling convention)
            if (function.getParameters().contains(varName)) {
                continue;
            }
            
            int size = getTypeSize(varType);
            stackOffset += size;
            variableOffsets.put(varName, -stackOffset);
            
            System.out.println("      [VAR] " + varName + " (" + varType + ") at offset " + (-stackOffset));
        }
        
        // Align stack to 16 bytes
        if (stackOffset % 16 != 0) {
            stackOffset += 16 - (stackOffset % 16);
        }
        
        if (stackOffset > 0) {
            assembly.append("    sub rsp, ").append(stackOffset).append("\n");
            System.out.println("    [STACK] Allocated " + stackOffset + " bytes for local variables");
        }
        
        // Handle parameters (assuming standard calling convention)
        List<String> params = function.getParameters();
        String[] paramRegs = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
        
        for (int i = 0; i < params.size() && i < paramRegs.length; i++) {
            String paramName = params.get(i);
            stackOffset += 8; // Assume 8 bytes for parameters
            variableOffsets.put(paramName, -stackOffset);
            assembly.append("    mov [rbp-").append(stackOffset).append("], ").append(paramRegs[i]).append("\n");
            System.out.println("      [PARAM] " + paramName + " stored at offset " + (-stackOffset));
        }
    }
    
    private void generateInstruction(Instruction instruction) {
        String instrType = instruction.getClass().getSimpleName();
        System.out.println("    [INSTR] Generating " + instrType);
        
        // Handle labels
        if (instruction.getLabel() != null) {
            assembly.append(instruction.getLabel()).append(":\n");
            usedLabels.add(instruction.getLabel());
        }
        
        if (instruction instanceof BinaryOperation) {
            generateBinaryOperation((BinaryOperation) instruction);
        } else if (instruction instanceof UnaryOperation) {
            generateUnaryOperation((UnaryOperation) instruction);
        } else if (instruction instanceof Copy) {
            generateCopy((Copy) instruction);
        } else if (instruction instanceof FunctionCall) {
            generateFunctionCall((FunctionCall) instruction);
        } else if (instruction instanceof Jump) {
            generateJump((Jump) instruction);
        } else if (instruction instanceof ConditionalJump) {
            generateConditionalJump((ConditionalJump) instruction);
        } else if (instruction instanceof Return) {
            generateReturn((Return) instruction);
        }
    }
    
    private void generateBinaryOperation(BinaryOperation op) {
        String result = op.getResult();
        String left = op.getLeft();
        String right = op.getRight();
        String operator = op.getOperator();
        
        System.out.println("      [BINARY] " + result + " = " + left + " " + operator + " " + right);
        
        // Load left operand into rax
        loadToRegister(left, "rax");
        
        // Perform operation with right operand
        switch (operator) {
            case "+":
                if (isImmediate(right)) {
                    assembly.append("    add rax, ").append(right).append("\n");
                } else {
                    assembly.append("    add rax, ").append(getOperandString(right)).append("\n");
                }
                break;
            case "-":
                if (isImmediate(right)) {
                    assembly.append("    sub rax, ").append(right).append("\n");
                } else {
                    assembly.append("    sub rax, ").append(getOperandString(right)).append("\n");
                }
                break;
            case "*":
                if (isImmediate(right)) {
                    assembly.append("    imul rax, ").append(right).append("\n");
                } else {
                    assembly.append("    imul rax, ").append(getOperandString(right)).append("\n");
                }
                break;
            case "/":
                assembly.append("    cqo\n"); // Sign extend rax to rdx:rax
                if (isImmediate(right)) {
                    assembly.append("    mov rbx, ").append(right).append("\n");
                    assembly.append("    idiv rbx\n");
                } else {
                    assembly.append("    idiv ").append(getOperandString(right)).append("\n");
                }
                break;
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                generateComparison(operator, right);
                break;
        }
        
        // Store result
        storeFromRegister("rax", result);
    }
    
    private void generateComparison(String operator, String right) {
        if (isImmediate(right)) {
            assembly.append("    cmp rax, ").append(right).append("\n");
        } else {
            assembly.append("    cmp rax, ").append(getOperandString(right)).append("\n");
        }
        
        String setInstruction;
        switch (operator) {
            case "==": setInstruction = "sete"; break;
            case "!=": setInstruction = "setne"; break;
            case "<":  setInstruction = "setl"; break;
            case ">":  setInstruction = "setg"; break;
            case "<=": setInstruction = "setle"; break;
            case ">=": setInstruction = "setge"; break;
            default: setInstruction = "sete"; break;
        }
        
        assembly.append("    ").append(setInstruction).append(" al\n");
        assembly.append("    movzx rax, al\n"); // Zero-extend to full register
    }
    
    private void generateUnaryOperation(UnaryOperation op) {
        String result = op.getResult();
        String operand = op.getOperand();
        String operator = op.getOperator();
        
        System.out.println("      [UNARY] " + result + " = " + operator + " " + operand);
        
        loadToRegister(operand, "rax");
        
        switch (operator) {
            case "-":
                assembly.append("    neg rax\n");
                break;
            case "!":
                assembly.append("    test rax, rax\n");
                assembly.append("    setz al\n");
                assembly.append("    movzx rax, al\n");
                break;
        }
        
        storeFromRegister("rax", result);
    }
    
    private void generateCopy(Copy copy) {
        String dest = copy.getDestination();
        String src = copy.getSource();
        
        // Skip nop operations used for labels
        if ("nop".equals(dest) && "nop".equals(src)) {
            return;
        }
        
        System.out.println("      [COPY] " + dest + " = " + src);
        
        if (isImmediate(src)) {
            assembly.append("    mov rax, ").append(src).append("\n");
        } else {
            loadToRegister(src, "rax");
        }
        
        storeFromRegister("rax", dest);
    }
    
    private void generateFunctionCall(FunctionCall call) {
        String result = call.getResult();
        String function = call.getFunction();
        List<String> arguments = call.getArguments();
        
        System.out.println("      [CALL] " + function + " with " + arguments.size() + " arguments");
        
        // Standard x86-64 calling convention: rdi, rsi, rdx, rcx, r8, r9
        String[] argRegs = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
        
        // Load arguments into registers
        for (int i = 0; i < arguments.size() && i < argRegs.length; i++) {
            loadToRegister(arguments.get(i), argRegs[i]);
        }
        
        // Call function
        assembly.append("    call ").append(function).append("\n");
        
        // Store result if function returns a value
        if (result != null && !result.equals("void")) {
            storeFromRegister("rax", result);
        }
    }
    
    private void generateJump(Jump jump) {
        String target = jump.getTarget();
        System.out.println("      [JUMP] goto " + target);
        assembly.append("    jmp ").append(target).append("\n");
    }
    
    private void generateConditionalJump(ConditionalJump condJump) {
        String condition = condJump.getCondition();
        String trueTarget = condJump.getTrueTarget();
        String falseTarget = condJump.getFalseTarget();
        
        System.out.println("      [COND_JUMP] if " + condition + " goto " + trueTarget + " else goto " + falseTarget);
        
        // Load condition and test
        loadToRegister(condition, "rax");
        assembly.append("    test rax, rax\n");
        assembly.append("    jnz ").append(trueTarget).append("\n");
        assembly.append("    jmp ").append(falseTarget).append("\n");
    }
    
    private void generateReturn(Return ret) {
        String value = ret.getValue();
        
        if (value != null) {
            System.out.println("      [RETURN] return " + value);
            loadToRegister(value, "rax");
        } else {
            System.out.println("      [RETURN] return (void)");
        }
        
        assembly.append("    mov rsp, rbp\n");
        assembly.append("    pop rbp\n");
        assembly.append("    ret\n");
    }
    
    private void loadToRegister(String operand, String register) {
        if (isImmediate(operand)) {
            assembly.append("    mov ").append(register).append(", ").append(operand).append("\n");
        } else if (operand.startsWith("str")) {
            // String literal
            assembly.append("    lea ").append(register).append(", [").append(operand).append("]\n");
        } else {
            // Variable
            String operandStr = getOperandString(operand);
            assembly.append("    mov ").append(register).append(", ").append(operandStr).append("\n");
        }
    }
    
    private void storeFromRegister(String register, String destination) {
        String destStr = getOperandString(destination);
        assembly.append("    mov ").append(destStr).append(", ").append(register).append("\n");
    }
    
    private String getOperandString(String operand) {
        if (variableOffsets.containsKey(operand)) {
            int offset = variableOffsets.get(operand);
            return "[rbp" + (offset >= 0 ? "+" : "") + offset + "]";
        } else if (operand.startsWith("str")) {
            return operand;
        } else {
            // If not found in variables, assume it's a temporary that should be on stack
            // This is a fallback - in a real compiler we'd track all temporaries
            return "[rbp-8]"; // Default location
        }
    }
    
    private boolean isImmediate(String operand) {
        return operand.matches("-?\\d+") || operand.matches("-?\\d+\\.\\d+");
    }
    
    private int getTypeSize(String type) {
        switch (type) {
            case "int":
            case "float":
                return 4;
            case "long":
            case "double":
            case "string":
                return 8;
            default:
                return 8; // Default to 8 bytes
        }
    }
    
    private void writeToFile(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(assembly.toString());
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        // This would typically be called from your main compiler driver
        System.out.println("CodeGenerator ready for use");
        System.out.println("Usage: Pass ThreeAddressCode IR to generateCode() method");
    }
}