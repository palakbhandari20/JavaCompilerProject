import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Lexer {
    static class Token {
        String type;
        String value;
        int line;

        Token(String type, String value, int line) {
            this.type = type;
            this.value = value;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Line " + line + ": <" + type + ", " + value + ">";
        }
    }

    // Java keywords
    static final Set<String> keywords = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", 
        "class", "const", "continue", "default", "do", "double", "else", "enum", 
        "extends", "final", "finally", "float", "for", "goto", "if", "implements", 
        "import", "instanceof", "int", "interface", "long", "native", "new", 
        "package", "private", "protected", "public", "return", "short", "static", 
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", 
        "transient", "try", "void", "volatile", "while"
    ));

    // Patterns for different tokens
    static final String identifier = "[a-zA-Z_][a-zA-Z0-9_]*";
    static final String number = "\\d+(\\.\\d+)?";
    static final String stringLiteral = "\".*?\"";
    static final String operator = "\\+|\\-|\\*|\\/|==|=|!=|<=|>=|<|>";
    static final String punctuation = "[\\{\\}\\(\\)\\[\\];,\\.]";

    public static List<Token> tokenize(String filename) {
        List<Token> tokens = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNum = 1;

            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+|(?=[{}();,])|(?<=[{}();,])");
                for (String part : parts) {
                    if (part.isEmpty()) continue;

                    if (keywords.contains(part)) {
                        tokens.add(new Token("KEYWORD", part, lineNum));
                    } else if (part.matches(identifier)) {
                        tokens.add(new Token("IDENTIFIER", part, lineNum));
                    } else if (part.matches(number)) {
                        tokens.add(new Token("NUMBER", part, lineNum));
                    } else if (part.matches(stringLiteral)) {
                        tokens.add(new Token("STRING", part, lineNum));
                    } else if (part.matches(operator)) {
                        tokens.add(new Token("OPERATOR", part, lineNum));
                    } else if (part.matches(punctuation)) {
                        tokens.add(new Token("PUNCTUATION", part, lineNum));
                    } else {
                        tokens.add(new Token("UNKNOWN", part, lineNum));
                    }
                }
                lineNum++;
            }

        } catch (IOException e) {
            System.out.println("File not found: " + filename);
        }

        return tokens;
    }

    // For quick testing
    public static void main(String[] args) {
        String filename = "Sample.java"; // You can change this
        List<Token> tokens = tokenize(filename);

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
