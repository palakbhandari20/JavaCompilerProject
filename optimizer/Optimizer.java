package optimizer;

import ir.ThreeAddressCode;
import ir.ThreeAddressCode.*;

import java.util.*;

public class Optimizer {

    public ThreeAddressCode optimize(ThreeAddressCode ir) {
        ThreeAddressCode optimizedIR = new ThreeAddressCode();

        for (Function func : ir.getFunctions()) {
            Function optimizedFunc = new Function(func.getName(), func.getReturnType());
            for (String param : func.getParameters()) {
                optimizedFunc.addParameter(param, func.getVariables().get(param));
            }
            for (Map.Entry<String, String> var : func.getVariables().entrySet()) {
                optimizedFunc.addVariable(var.getKey(), var.getValue());
            }

            List<Instruction> instructions = func.getInstructions();

            // Apply constant folding and propagation
            List<Instruction> folded = constantFoldingAndPropagation(instructions);

            // Print before DCE
            System.out.println("Instructions before Dead Code Elimination (DCE):");
            for (Instruction inst : folded) {
                System.out.println(inst);
            }

            // Apply dead code elimination
            List<Instruction> cleaned = deadCodeElimination(folded);

            // Print after DCE
            System.out.println("Instructions after Dead Code Elimination (DCE):");
            for (Instruction inst : cleaned) {
                System.out.println(inst);
                optimizedFunc.addInstruction(inst);
            }

            optimizedIR.addFunction(optimizedFunc);
        }

        return optimizedIR;
    }

    private List<Instruction> constantFoldingAndPropagation(List<Instruction> instructions) {
        Map<String, String> constants = new HashMap<>();
        List<Instruction> result = new ArrayList<>();

        for (Instruction inst : instructions) {
            if (inst instanceof Copy copy) {
                String src = copy.getSource();
                if (constants.containsKey(src)) {
                    src = constants.get(src);
                }
                result.add(new Copy(copy.getDestination(), src));

                if (isNumeric(src)) {
                    constants.put(copy.getDestination(), src);
                } else {
                    constants.remove(copy.getDestination());
                }

            } else if (inst instanceof BinaryOperation binOp) {
                String left = binOp.getLeft();
                String right = binOp.getRight();

                if (constants.containsKey(left)) left = constants.get(left);
                if (constants.containsKey(right)) right = constants.get(right);

                if (isNumeric(left) && isNumeric(right)) {
                    int foldedValue = evaluate(Integer.parseInt(left), binOp.getOperator(), Integer.parseInt(right));
                    result.add(new Copy(binOp.getResult(), Integer.toString(foldedValue)));
                    constants.put(binOp.getResult(), Integer.toString(foldedValue));
                } else {
                    result.add(new BinaryOperation(binOp.getResult(), left, binOp.getOperator(), right));
                    constants.remove(binOp.getResult());
                }

            } else {
                result.add(inst);
            }
        }

        return result;
    }

    private List<Instruction> deadCodeElimination(List<Instruction> instructions) {
        Set<String> usedVars = new HashSet<>();
        List<Instruction> optimized = new ArrayList<>();

        // Step 1: Find used variables (backward pass)
        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instruction inst = instructions.get(i);

            if (inst instanceof BinaryOperation binOp) {
                if (usedVars.contains(binOp.getResult()) || !isTemp(binOp.getResult())) {
                    usedVars.add(binOp.getLeft());
                    usedVars.add(binOp.getRight());
                    optimized.add(0, inst);
                }
            } else if (inst instanceof Copy copy) {
                if (usedVars.contains(copy.getDestination()) || !isTemp(copy.getDestination())) {
                    usedVars.add(copy.getSource());
                    optimized.add(0, inst);
                }
            } else if (inst instanceof Return ret) {
                if (ret.getValue() != null) {
                    usedVars.add(ret.getValue());
                }
                optimized.add(0, inst);
            } else if (inst instanceof UnaryOperation unary) {
                if (usedVars.contains(unary.getResult()) || !isTemp(unary.getResult())) {
                    usedVars.add(unary.getOperand());
                    optimized.add(0, inst);
                }
            } else if (inst instanceof ConditionalJump cj) {
                usedVars.add(cj.getCondition());
                optimized.add(0, inst);
            } else if (inst instanceof FunctionCall call) {
                if (call.getResult() != null && (usedVars.contains(call.getResult()) || !isTemp(call.getResult()))) {
                    for (String arg : call.getArguments()) {
                        usedVars.add(arg);
                    }
                    optimized.add(0, inst);
                } else if (call.getResult() == null) {
                    for (String arg : call.getArguments()) {
                        usedVars.add(arg);
                    }
                    optimized.add(0, inst);
                }
            } else {
                optimized.add(0, inst); // Always retain Jumps and labels
            }
        }

        return optimized;
    }

    private boolean isNumeric(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int evaluate(int left, String op, int right) {
        return switch (op) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> (right != 0) ? left / right : 0;
            default -> 0;
        };
    }

    private boolean isTemp(String var) {
        return var != null && var.matches("t\\d+");
    }
}
