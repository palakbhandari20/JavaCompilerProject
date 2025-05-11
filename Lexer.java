import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical Analyzer (Lexer)
 * Converts source code into tokens
 */
public class Lexer {
    // Token types
    public enum TokenType {
        // Keywords
        CLASS, PUBLIC, PRIVATE, STATIC, VOID, INT, BOOLEAN, STRING,
        IF, ELSE, WHILE, RETURN, NEW, THIS,
        
        // Operators
        PLUS, MINUS, MULTIPLY, DIVIDE, ASSIGN, LT, GT, EQ, AND, OR, NOT,
        
        // Delimiters
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, SEMICOLON, COMMA, DOT,
        
        // Literals
        IDENTIFIER, INTEGER_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL,
        
        // Special
        EOF
    }
    
    // Token class
    public static class Token {
        public TokenType type;
        public String value;
        public int line;
        public int column;
        
        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }
        
        @Override
        public String toString() {
            return String.format("Token[type=%s, value='%s', position=(%d,%d)]", 
                    type, value, line, column);
        }
    }
    
    // Tokenize source code
    public static List<Token> tokenize(String filename) {
        System.out.println("Tokenizing source file: " + filename);
        List<Token> tokens = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                // Remove comments
                line = line.replaceAll("//.*$", "");
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                
                // Process the line
                tokens.addAll(tokenizeLine(line, lineNumber));
                lineNumber++;
            }
            
            // Add EOF token
            tokens.add(new Token(TokenType.EOF, "", lineNumber, 1));
            
            System.out.println("Tokenization complete. Generated " + tokens.size() + " tokens.");
            return tokens;
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Tokenize a single line
    private static List<Token> tokenizeLine(String line, int lineNumber) {
        List<Token> lineTokens = new ArrayList<>();
        int column = 1;
        
        // Define regular expressions for different token types
        Pattern pattern = Pattern.compile(
            // Keywords
            "\\b(class|public|private|static|void|int|boolean|String|if|else|while|return|new|this)\\b|" +
            // Operators
            "(\\+|-|\\*|/|=|<|>|==|&&|\\|\\||!)|" +
            // Delimiters
            "(\\(|\\)|\\{|\\}|\\[|\\]|;|,|\\.)|" +
            // Literals
            "\\b(true|false)\\b|" +
            "\"([^\"]*)\"|\\'([^\\']*)\\'|" +
            "\\b(\\d+)\\b|" +
            "\\b([a-zA-Z][a-zA-Z0-9_]*)\\b"
        );
        
        Matcher matcher = pattern.matcher(line);
        
        while (matcher.find()) {
            String lexeme = matcher.group(0);
            column = matcher.start() + 1;
            
            // Determine token type
            TokenType type = determineTokenType(lexeme);
            
            // Add token to the list
            lineTokens.add(new Token(type, lexeme, lineNumber, column));
        }
        
        return lineTokens;
    }
    
    // Determine the type of a token based on its lexeme
    private static TokenType determineTokenType(String lexeme) {
        // Keywords
        switch (lexeme) {
            case "class": return TokenType.CLASS;
            case "public": return TokenType.PUBLIC;
            case "private": return TokenType.PRIVATE;
            case "static": return TokenType.STATIC;
            case "void": return TokenType.VOID;
            case "int": return TokenType.INT;
            case "boolean": return TokenType.BOOLEAN;
            case "String": return TokenType.STRING;
            case "if": return TokenType.IF;
            case "else": return TokenType.ELSE;
            case "while": return TokenType.WHILE;
            case "return": return TokenType.RETURN;
            case "new": return TokenType.NEW;
            case "this": return TokenType.THIS;
            case "true":
            case "false": return TokenType.BOOLEAN_LITERAL;
        }
        
        // Operators
        switch (lexeme) {
            case "+": return TokenType.PLUS;
            case "-": return TokenType.MINUS;
            case "*": return TokenType.MULTIPLY;
            case "/": return TokenType.DIVIDE;
            case "=": return TokenType.ASSIGN;
            case "<": return TokenType.LT;
            case ">": return TokenType.GT;
            case "==": return TokenType.EQ;
            case "&&": return TokenType.AND;
            case "||": return TokenType.OR;
            case "!": return TokenType.NOT;
        }
        
        // Delimiters
        switch (lexeme) {
            case "(": return TokenType.LPAREN;
            case ")": return TokenType.RPAREN;
            case "{": return TokenType.LBRACE;
            case "}": return TokenType.RBRACE;
            case "[": return TokenType.LBRACKET;
            case "]": return TokenType.RBRACKET;
            case ";": return TokenType.SEMICOLON;
            case ",": return TokenType.COMMA;
            case ".": return TokenType.DOT;
        }
        
        // Literals
        if (lexeme.matches("\\d+")) {
            return TokenType.INTEGER_LITERAL;
        } else if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
            return TokenType.STRING_LITERAL;
        } else if (lexeme.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            return TokenType.IDENTIFIER;
        }
        
        // Default
        return TokenType.IDENTIFIER; // Fallback
    }
    
    // Main method for testing
    public static void main(String[] args) {
        String filename = "Sample.java";
        List<Token> tokens = tokenize(filename);
        
        System.out.println("\nTokens:");
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}