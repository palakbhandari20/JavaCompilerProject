package backend;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import frontend.ir.IRInstruction;
import frontend.ir.IRParser;

public class CodeGenerator {
    private final List<IRInstruction> instructions;
    private final StringBuilder assembly;
    private final Map<String, Integer> variableOffsets;
    private int stackOffset;

    public CodeGenerator(String irFilePath) throws IOException {
        this.instructions = IRParser.parseIR(irFilePath);
        this.assembly = new StringBuilder();
        this.variableOffsets = new HashMap<>();
        this.stackOffset = 0;
    }

    public void generate(String outputPath) throws IOException {
        emitHeader();
        allocateStackSpace();
        for (IRInstruction instruction : instructions) {
            generateInstruction(instruction);
        }
        emitFooter();
        writeToFile(outputPath);
    }

    private void emitHeader() {
        assembly.append("section .text\n");
        assembly.append("global _start\n\n");
        assembly.append("_start:\n");
        assembly.append("    push rbp\n");
        assembly.append("    mov rbp, rsp\n");
    }

    private void allocateStackSpace() {
        for (IRInstruction instr : instructions) {
            for (String operand : instr.getOperands()) {
                if (!isImmediate(operand) && !variableOffsets.containsKey(operand)) {
                    stackOffset -= 4;
                    variableOffsets.put(operand, stackOffset);
                }
            }
        }
        if (stackOffset != 0) {
            assembly.append("    sub rsp, ").append(-stackOffset).append("\n");
        }
    }

    private void emitFooter() {
        assembly.append("    mov rsp, rbp\n");
        assembly.append("    pop rbp\n");
        assembly.append("    mov rax, 60\n");  // syscall: exit
        assembly.append("    xor rdi, rdi\n"); // status 0
        assembly.append("    syscall\n");
    }

    private void generateInstruction(IRInstruction instr) {
        String op = instr.getOperation();
        List<String> ops = instr.getOperands();

        switch (op) {
            case "MOV":
                generateMov(ops.get(0), ops.get(1));
                break;
            case "ADD":
                generateAdd(ops.get(0), ops.get(1), ops.get(2));
                break;
            case "SUB":
                generateSub(ops.get(0), ops.get(1), ops.get(2));
                break;
            case "MUL":
                generateMul(ops.get(0), ops.get(1), ops.get(2));
                break;
            case "DIV":
                generateDiv(ops.get(0), ops.get(1), ops.get(2));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation: " + op);
        }
    }

    private void generateMov(String dest, String src) {
        if (isImmediate(src)) {
            assembly.append("    mov dword ").append(getOperandString(dest)).append(", ").append(src).append("\n");
        } else {
            assembly.append("    mov eax, dword ").append(getOperandString(src)).append("\n");
            assembly.append("    mov dword ").append(getOperandString(dest)).append(", eax\n");
        }
    }

    private void generateAdd(String dest, String op1, String op2) {
        if (isImmediate(op1)) {
            assembly.append("    mov eax, ").append(op1).append("\n");
        } else {
            assembly.append("    mov eax, dword ").append(getOperandString(op1)).append("\n");
        }

        if (isImmediate(op2)) {
            assembly.append("    add eax, ").append(op2).append("\n");
        } else {
            assembly.append("    add eax, dword ").append(getOperandString(op2)).append("\n");
        }

        assembly.append("    mov dword ").append(getOperandString(dest)).append(", eax\n");
    }

    private void generateSub(String dest, String op1, String op2) {
        if (isImmediate(op1)) {
            assembly.append("    mov eax, ").append(op1).append("\n");
        } else {
            assembly.append("    mov eax, dword ").append(getOperandString(op1)).append("\n");
        }

        if (isImmediate(op2)) {
            assembly.append("    sub eax, ").append(op2).append("\n");
        } else {
            assembly.append("    sub eax, dword ").append(getOperandString(op2)).append("\n");
        }

        assembly.append("    mov dword ").append(getOperandString(dest)).append(", eax\n");
    }

    private void generateMul(String dest, String op1, String op2) {
        if (isImmediate(op1)) {
            assembly.append("    mov eax, ").append(op1).append("\n");
        } else {
            assembly.append("    mov eax, dword ").append(getOperandString(op1)).append("\n");
        }

        if (isImmediate(op2)) {
            assembly.append("    imul eax, ").append(op2).append("\n");
        } else {
            assembly.append("    imul eax, dword ").append(getOperandString(op2)).append("\n");
        }

        assembly.append("    mov dword ").append(getOperandString(dest)).append(", eax\n");
    }

    private void generateDiv(String dest, String op1, String op2) {
        // Clear rdx for division
        assembly.append("    xor edx, edx\n");

        if (isImmediate(op1)) {
            assembly.append("    mov eax, ").append(op1).append("\n");
        } else {
            assembly.append("    mov eax, dword ").append(getOperandString(op1)).append("\n");
        }

        if (isImmediate(op2)) {
            assembly.append("    mov ebx, ").append(op2).append("\n");
        } else {
            assembly.append("    mov ebx, dword ").append(getOperandString(op2)).append("\n");
        }

        assembly.append("    idiv ebx\n");
        assembly.append("    mov dword ").append(getOperandString(dest)).append(", eax\n");
    }

    private String getOperandString(String operand) {
        if (variableOffsets.containsKey(operand)) {
            int offset = variableOffsets.get(operand);
            return "[rbp" + (offset < 0 ? offset : "+" + offset) + "]";
        } else {
            throw new RuntimeException("[ERROR] Unknown operand: " + operand);
        }
    }

    private boolean isImmediate(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void writeToFile(String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(assembly.toString());
        }
    }
}
