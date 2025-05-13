/*
 * File: src/compiler/codegen/CodeGenerator.java
 * Generates assembly code from optimized intermediate representation
 */
package codegen;

import ir.ThreeAddressCode;
import ir.ThreeAddressCode.Function;
import ir.ThreeAddressCode.Instruction;
import ir.ThreeAddressCode.BinaryOperation;
import ir.ThreeAddressCode.Copy;
import ir.ThreeAddressCode.UnaryOperation;
import ir.ThreeAddressCode.FunctionCall;
import ir.ThreeAddressCode.ConditionalJump;
import ir.ThreeAddressCode.Return;
import ir.ThreeAddressCode.Jump;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {
    private Map<String, String> stringLiterals;
    
    public CodeGenerator() {
        this.stringLiterals = new HashMap<>();
    }
    
    public String generate(ThreeAddressCode ir) {
        StringBuilder assemblyCode = new StringBuilder();
        
        // Generate assembly code header
        generateHeader(assemblyCode);
        
        // Generate data section for string literals
        generateDataSection(assemblyCode);
        
        // Generate text section with code
        generateTextSection(assemblyCode, ir);
        
        return assemblyCode.toString();
    }
    
    private void generateHeader(StringBuilder assemblyCode) {
        assemblyCode.append("; Compiler Generated Assembly\n");
        assemblyCode.append("; Target: x86-64 Linux\n\n");
    }
    
    private void generateDataSection(StringBuilder assemblyCode) {
        if (!stringLiterals.isEmpty()) {
            assemblyCode.append("section .data\n");
            for (Map.Entry<String, String> literal : stringLiterals.entrySet()) {
                String label = literal.getKey();
                String value = literal.getValue();
                assemblyCode.append("    ").append(label).append(": db ")
                        .append('"').append(escapeString(value)).append('"')
                        .append(", 0\n");
            }
            assemblyCode.append("\n");
        }
    }
    
    private void generateTextSection(StringBuilder assemblyCode, ThreeAddressCode ir) {
        assemblyCode.append("section .text\n");
        assemblyCode.append("global _start\n\n");
        
        // Generate code for each function
        for (Function function : ir.getFunctions()) {
            generateFunction(assemblyCode, function);
        }
        
        // Add program entry point
        generateStartFunction(assemblyCode);
    }
    
    private void generateFunction(StringBuilder assemblyCode, Function function) {
        // Function label
        assemblyCode.append(function.getName()).append(":\n");
        
        // Function prologue
        assemblyCode.append("    push rbp\n");
        assemblyCode.append("    mov rbp, rsp\n");
        
        // Allocate stack space for local variables
        int localVarSize = calculateLocalVarSize(function);
        if (localVarSize > 0) {
            assemblyCode.append("    sub rsp, ").append(localVarSize).append("\n");
        }
        
        // Generate code for function body
        Map<String, Integer> variableOffsets = calculateVariableOffsets(function);
        for (Instruction inst : function.getInstructions()) {
            generateInstruction(assemblyCode, inst, variableOffsets);
        }
        
        // Function epilogue
        assemblyCode.append(".end_").append(function.getName()).append(":\n");
        assemblyCode.append("    mov rsp, rbp\n");
        assemblyCode.append("    pop rbp\n");
        assemblyCode.append("    ret\n\n");
    }
    
    private void generateStartFunction(StringBuilder assemblyCode) {
        // Entry point for the program
        assemblyCode.append("_start:\n");
        assemblyCode.append("    ; Call main function\n");
        assemblyCode.append("    call main\n");
        
        // Exit system call
        assemblyCode.append("    ; Exit program\n");
        assemblyCode.append("    mov rdi, rax\n");  // Use return value from main as exit code
        assemblyCode.append("    mov rax, 60\n");   // syscall number for exit
        assemblyCode.append("    syscall\n");
    }
    
    private void generateInstruction(StringBuilder assemblyCode, 
                                     Instruction inst, 
                                     Map<String, Integer> variableOffsets) {
        // Handle label if present
        if (inst.getLabel() != null) {
            assemblyCode.append(".").append(inst.getLabel()).append(":\n");
        }
        
        // Generate code based on instruction type
        if (inst instanceof BinaryOperation) {
            generateBinaryOperation(assemblyCode, (BinaryOperation) inst, variableOffsets);
        } else if (inst instanceof Copy) {
            generateCopy(assemblyCode, (Copy) inst, variableOffsets);
        } else if (inst instanceof UnaryOperation) {
            generateUnaryOperation(assemblyCode, (UnaryOperation) inst, variableOffsets);
        } else if (inst instanceof FunctionCall) {
            generateFunctionCall(assemblyCode, (FunctionCall) inst, variableOffsets);
        } else if (inst instanceof ConditionalJump) {
            generateConditionalJump(assemblyCode, (ConditionalJump) inst, variableOffsets);
        } else if (inst instanceof Jump) {
            generateJump(assemblyCode, (Jump) inst);
        } else if (inst instanceof Return) {
            generateReturn(assemblyCode, (Return) inst, variableOffsets);
        }
    }
    
    private void generateBinaryOperation(StringBuilder assemblyCode, 
                                         BinaryOperation op, 
                                         Map<String, Integer> variableOffsets) {
        // Load left operand to rax
        loadVariable(assemblyCode, op.getLeft(), "rax", variableOffsets);
        
        // Load right operand to rcx
        loadVariable(assemblyCode, op.getRight(), "rcx", variableOffsets);
        
        // Perform operation based on operator
        switch (op.getOperator()) {
            case "+":
                assemblyCode.append("    add rax, rcx\n");
                break;
            case "-":
                assemblyCode.append("    sub rax, rcx\n");
                break;
            case "*":
                assemblyCode.append("    imul rax, rcx\n");
                break;
            case "/":
                assemblyCode.append("    xor rdx, rdx\n");  // Clear rdx for division
                assemblyCode.append("    idiv rcx\n");
                break;
            case "==":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    sete al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
            case "!=":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    setne al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
            case "<":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    setl al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
            case ">":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    setg al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
            case "<=":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    setle al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
            case ">=":
                assemblyCode.append("    cmp rax, rcx\n");
                assemblyCode.append("    setge al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
        }
        
        // Store result
        storeVariable(assemblyCode, op.getResult(), "rax", variableOffsets);
    }
    
    private void generateCopy(StringBuilder assemblyCode, 
                               Copy copy, 
                               Map<String, Integer> variableOffsets) {
        // Skip NOP instructions
        if (copy.getDestination().equals("nop") && copy.getSource().equals("nop")) {
            return;
        }
        
        // Load source to rax
        loadVariable(assemblyCode, copy.getSource(), "rax", variableOffsets);
        
        // Store to destination
        storeVariable(assemblyCode, copy.getDestination(), "rax", variableOffsets);
    }
    
    private void generateUnaryOperation(StringBuilder assemblyCode, 
                                        UnaryOperation op, 
                                        Map<String, Integer> variableOffsets) {
        // Load operand to rax
        loadVariable(assemblyCode, op.getOperand(), "rax", variableOffsets);
        
        // Perform operation
        switch (op.getOperator()) {
            case "-":
                assemblyCode.append("    neg rax\n");
                break;
            case "!":
                assemblyCode.append("    test rax, rax\n");
                assemblyCode.append("    setz al\n");
                assemblyCode.append("    movzx rax, al\n");
                break;
        }
        
        // Store result
        storeVariable(assemblyCode, op.getResult(), "rax", variableOffsets);
    }
    
    private void generateFunctionCall(StringBuilder assemblyCode, 
                                      FunctionCall call, 
                                      Map<String, Integer> variableOffsets) {
        // Push arguments in reverse order (right to left)
        List<String> arguments = call.getArguments();
        for (int i = arguments.size() - 1; i >= 0; i--) {
            loadVariable(assemblyCode, arguments.get(i), "rax", variableOffsets);
            assemblyCode.append("    push rax\n");
        }
        
        // Call the function
        assemblyCode.append("    call ").append(call.getFunction()).append("\n");
        
        // Clean up stack
        if (!arguments.isEmpty()) {
            assemblyCode.append("    add rsp, ").append(arguments.size() * 8).append("\n");
        }
        
        // Store return value if result is expected
        if (call.getResult() != null) {
            storeVariable(assemblyCode, call.getResult(), "rax", variableOffsets);
        }
    }
    
    private void generateConditionalJump(StringBuilder assemblyCode, 
                                         ConditionalJump jump, 
                                         Map<String, Integer> variableOffsets) {
        // Load condition to rax
        loadVariable(assemblyCode, jump.getCondition(), "rax", variableOffsets);
        
        // Test condition
        assemblyCode.append("    test rax, rax\n");
        assemblyCode.append("    jnz .").append(jump.getTrueTarget()).append("\n");
        assemblyCode.append("    jmp .").append(jump.getFalseTarget()).append("\n");
    }
    
    private void generateJump(StringBuilder assemblyCode, Jump jump) {
        assemblyCode.append("    jmp .").append(jump.getTarget()).append("\n");
    }
    
    private void generateReturn(StringBuilder assemblyCode, 
                                Return ret, 
                                Map<String, Integer> variableOffsets) {
        if (ret.getValue() != null) {
            // Load return value to rax
            loadVariable(assemblyCode, ret.getValue(), "rax", variableOffsets);
        } else {
            // Return 0 by default
            assemblyCode.append("    xor rax, rax\n");
        }
        
        // Jump to function epilogue
        assemblyCode.append("    jmp .end_").append("main").append("\n");
    }
    
    private void loadVariable(StringBuilder assemblyCode, 
                               String variable, 
                               String register, 
                               Map<String, Integer> variableOffsets) {
        // Handle numeric literals
        if (variable.matches("-?\\d+")) {
            assemblyCode.append("    mov ").append(register).append(", ").append(variable).append("\n");
            return;
        }
        
        // Handle string literals
        if (stringLiterals.containsKey(variable)) {
            assemblyCode.append("    mov ").append(register).append(", ").append(variable).append("\n");
            return;
        }
        
        // Handle variables
        if (variableOffsets.containsKey(variable)) {
            int offset = variableOffsets.get(variable);
            assemblyCode.append("    mov ").append(register).append(", [rbp-")
                    .append(offset).append("]\n");
        }
    }
    
    private void storeVariable(StringBuilder assemblyCode, 
                                String variable, 
                                String register, 
                                Map<String, Integer> variableOffsets) {
        // Skip NOP instructions
        if (variable.equals("nop")) {
            return;
        }
        
        // Handle string and numeric literals (they are constants)
        if (variable.matches("-?\\d+") || stringLiterals.containsKey(variable)) {
            return;
        }
        
        // Store to variable
        if (variableOffsets.containsKey(variable)) {
            int offset = variableOffsets.get(variable);
            assemblyCode.append("    mov [rbp-").append(offset).append("], ").append(register).append("\n");
        }
    }
    
    private int calculateLocalVarSize(Function function) {
        // Align to 16 bytes
        int size = function.getVariables().size() * 8;
        return (size + 15) & ~15;
    }
    
    private Map<String, Integer> calculateVariableOffsets(Function function) {
        Map<String, Integer> offsets = new HashMap<>();
        int currentOffset = 0;
        
        // Allocate space for local variables
        for (Map.Entry<String, String> var : function.getVariables().entrySet()) {
            if (!function.getParameters().contains(var.getKey())) {
                currentOffset += 8;
                offsets.put(var.getKey(), currentOffset);
            }
        }
        
        return offsets;
    }
    
    private String escapeString(String str) {
        // Escape special characters in string literals
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

}