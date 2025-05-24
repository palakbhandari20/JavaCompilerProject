import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.AST;
import semantic.SemanticAnalyzer;
import ir.IRGenerator;
import ir.ThreeAddressCode;
import optimizer.Optimizer;
import codegen.CodeGenerator;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java compiler.Main <source-file>");
            return;
        }

        try {
            // Read the source file
            String sourceCode = new String(Files.readAllBytes(Paths.get(args[0])));
            
            // Phase 1: Lexical Analysis
            System.out.println("Phase 1: Lexical Analysis");
            Lexer lexer = new Lexer(sourceCode);

            // Phase 2: Syntax Analysis
            System.out.println("Phase 2: Syntax Analysis");
            List<Token> tokens = lexer.tokenize(); // assuming your Lexer has a method tokenize()

            System.out.println("Tokens:");
            for (Token token : tokens) {
                System.out.println(token.getType() + " -> " + token.getLexeme());
            }
            System.out.println("Total tokens: " + tokens.size());

            Parser parser = new Parser(tokens);
            AST.Program program = parser.parseProgram();

            
            // Phase 3: Semantic Analysis
            System.out.println("Phase 3: Semantic Analysis");
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(program);
            
            // Phase 4: Intermediate Code Generation
            System.out.println("Phase 4: Intermediate Code Generation");
            IRGenerator irGenerator = new IRGenerator();
            ThreeAddressCode ir = irGenerator.generate(program);
            
            // Phase 5: Code Optimization
            System.out.println("Phase 5: Code Optimization");
            Optimizer optimizer = new Optimizer();
            ThreeAddressCode optimizedIR = optimizer.optimize(ir);
            System.out.println("Optimized IR:");
            for (ThreeAddressCode.Function func : optimizedIR.getFunctions()) {
                System.out.println("Function: " + func.getName());
                for (var inst : func.getInstructions()) {
                    System.out.println(inst);
                }
            }
            
            // Phase 6: Code Generation
            System.out.println("Phase 6: Code Generation");
            CodeGenerator codeGenerator = new CodeGenerator();
            
            // Generate output file name
            String outputFile = args[0].substring(0, args[0].lastIndexOf('.')) + ".asm";
            
            // Generate assembly code - the method writes directly to file and returns void
            codeGenerator.generateCode(optimizedIR, outputFile);
            
            System.out.println("Compilation completed successfully. Output written to " + outputFile);
            
        } catch (IOException e) {
            System.err.println("Error reading source file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Compilation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}