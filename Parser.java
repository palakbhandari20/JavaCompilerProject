import java.util.List;

public class Parser {
    private List<Lexer.Token> tokens;
    private int position;
    private Lexer.Token currentToken;
    
    public Parser(List<Lexer.Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        if (!tokens.isEmpty()) {
            this.currentToken = tokens.get(0);
        } else {
            throw new RuntimeException("No tokens provided");
        }
    }
    
    // Moves to the next token
    private void advance() {
        position++;
        if (position < tokens.size()) {
            currentToken = tokens.get(position);
        } else {
            // End of tokens
            currentToken = null;
        }
    }
    
    // Print current token and advance
    private void consume() {
        System.out.println("Consuming: " + currentToken);
        advance();
    }
    
    // Main parsing method
    public void parse() {
        System.out.println("Starting parsing...");
        System.out.println("Total tokens: " + tokens.size());
        
        // Print all tokens first for debugging
        System.out.println("\nToken list:");
        for (int i = 0; i < tokens.size(); i++) {
            System.out.println(i + ": " + tokens.get(i));
        }
        
        System.out.println("\nBeginning parse process:");
        parseClass();
        System.out.println("Parsing completed successfully!");
    }
    
    // Parse class definition
    private void parseClass() {
        // Skip tokens until we find the class declaration
        while (currentToken != null && 
              !(currentToken.type.equals("KEYWORD") && currentToken.value.equals("class"))) {
            System.out.println("Skipping: " + currentToken);
            advance();
        }
        
        if (currentToken == null) {
            throw new RuntimeException("Class declaration not found");
        }
        
        // Consume 'class'
        consume();
        
        // Consume class name
        if (currentToken.type.equals("IDENTIFIER")) {
            consume();
        } else {
            throw new RuntimeException("Expected class name, got: " + currentToken);
        }
        
        // Consume opening brace
        if (currentToken != null && currentToken.value.equals("{")) {
            consume();
        } else {
            throw new RuntimeException("Expected '{', got: " + currentToken);
        }
        
        // Parse class body until closing brace
        parseClassBody();
        
        // Consume closing brace
        if (currentToken != null && currentToken.value.equals("}")) {
            consume();
        } else {
            throw new RuntimeException("Expected '}', got: " + currentToken);
        }
    }
    
    // Parse class body
    private void parseClassBody() {
        // Skip tokens until we see 'main' method or closing brace
        while (currentToken != null && 
              !(currentToken.type.equals("IDENTIFIER") && currentToken.value.equals("main")) && 
              !currentToken.value.equals("}")) {
            System.out.println("Skipping in class body: " + currentToken);
            advance();
        }
        
        if (currentToken == null) {
            throw new RuntimeException("Unexpected end of tokens in class body");
        }
        
        if (currentToken.value.equals("}")) {
            return; // End of class body
        }
        
        // Parse main method
        parseMainMethod();
    }
    
    // Parse main method
    private void parseMainMethod() {
        // Consume 'main'
        consume();
        
        // Consume opening parenthesis
        if (currentToken != null && currentToken.value.equals("(")) {
            consume();
        } else {
            throw new RuntimeException("Expected '(', got: " + currentToken);
        }
        
        // Skip until closing parenthesis
        while (currentToken != null && !currentToken.value.equals(")")) {
            System.out.println("Skipping in parameters: " + currentToken);
            advance();
        }
        
        // Consume closing parenthesis
        if (currentToken != null && currentToken.value.equals(")")) {
            consume();
        } else {
            throw new RuntimeException("Expected ')', got: " + currentToken);
        }
        
        // Consume opening brace
        if (currentToken != null && currentToken.value.equals("{")) {
            consume();
        } else {
            throw new RuntimeException("Expected '{', got: " + currentToken);
        }
        
        // Parse method body
        parseMethodBody();
        
        // Consume closing brace
        if (currentToken != null && currentToken.value.equals("}")) {
            consume();
        } else {
            throw new RuntimeException("Expected '}', got: " + currentToken);
        }
    }
    
    // Parse method body
    private void parseMethodBody() {
        // Skip tokens until closing brace
        while (currentToken != null && !currentToken.value.equals("}")) {
            // If we find a variable declaration
            if (currentToken.type.equals("KEYWORD") && 
                (currentToken.value.equals("int") || 
                 currentToken.value.equals("String") || 
                 currentToken.value.equals("boolean"))) {
                parseVariableDeclaration();
            }
            // If we find an if statement
            else if (currentToken.type.equals("KEYWORD") && currentToken.value.equals("if")) {
                parseIfStatement();
            }
            // Otherwise just skip
            else {
                System.out.println("Skipping in method body: " + currentToken);
                advance();
            }
        }
    }
    
    // Parse variable declaration: int x = 10;
    private void parseVariableDeclaration() {
        System.out.println("Parsing variable declaration: " + currentToken);
        
        // Consume type
        consume();
        
        // Consume variable name
        if (currentToken != null && currentToken.type.equals("IDENTIFIER")) {
            consume();
        } else {
            throw new RuntimeException("Expected variable name, got: " + currentToken);
        }
        
        // Consume equals sign if present
        if (currentToken != null && currentToken.value.equals("=")) {
            consume();
            
            // Consume value
            if (currentToken != null && 
                (currentToken.type.equals("NUMBER") || 
                 currentToken.type.equals("STRING") || 
                 currentToken.type.equals("IDENTIFIER"))) {
                consume();
            } else {
                throw new RuntimeException("Expected value, got: " + currentToken);
            }
        }
        
        // Consume semicolon
        if (currentToken != null && currentToken.value.equals(";")) {
            consume();
        } else {
            throw new RuntimeException("Expected ';', got: " + currentToken);
        }
    }
    
    // Parse if statement: if (x > 5) { ... }
    private void parseIfStatement() {
        System.out.println("Parsing if statement: " + currentToken);
        
        // Consume 'if'
        consume();
        
        // Consume opening parenthesis
        if (currentToken != null && currentToken.value.equals("(")) {
            consume();
        } else {
            throw new RuntimeException("Expected '(', got: " + currentToken);
        }
        
        // Skip condition tokens until closing parenthesis
        while (currentToken != null && !currentToken.value.equals(")")) {
            System.out.println("Skipping in condition: " + currentToken);
            advance();
        }
        
        // Consume closing parenthesis
        if (currentToken != null && currentToken.value.equals(")")) {
            consume();
        } else {
            throw new RuntimeException("Expected ')', got: " + currentToken);
        }
        
        // Consume opening brace
        if (currentToken != null && currentToken.value.equals("{")) {
            consume();
        } else {
            throw new RuntimeException("Expected '{', got: " + currentToken);
        }
        
        // Skip tokens in if body until closing brace
        while (currentToken != null && !currentToken.value.equals("}")) {
            System.out.println("Skipping in if body: " + currentToken);
            advance();
        }
        
        // Consume closing brace
        if (currentToken != null && currentToken.value.equals("}")) {
            consume();
        } else {
            throw new RuntimeException("Expected '}', got: " + currentToken);
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        String filename = "Sample.java";
        List<Lexer.Token> tokens = Lexer.tokenize(filename);
        Parser parser = new Parser(tokens);
        
        try {
            parser.parse();
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}