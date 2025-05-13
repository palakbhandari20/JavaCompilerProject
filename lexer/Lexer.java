package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private int position;
    private int line;
    private int column;
    private char currentChar;

    // Keywords mapping
    private static final Map<String, TokenType> KEYWORDS;

static {
    KEYWORDS = new HashMap<>();
    KEYWORDS.put("int", TokenType.INT);
    KEYWORDS.put("float", TokenType.FLOAT);
    KEYWORDS.put("if", TokenType.IF);
    KEYWORDS.put("else", TokenType.ELSE);
    KEYWORDS.put("while", TokenType.WHILE);
    KEYWORDS.put("return", TokenType.RETURN);
    KEYWORDS.put("void", TokenType.VOID);
    KEYWORDS.put("public", TokenType.PUBLIC);
    KEYWORDS.put("static", TokenType.STATIC);
    KEYWORDS.put("class", TokenType.CLASS);
    KEYWORDS.put("String", TokenType.STRING);
}

    public Lexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;

        if (!source.isEmpty()) {
            this.currentChar = source.charAt(0);
        } else {
            this.currentChar = '\0'; // Null character represents EOF
        }
    }

    private void advance() {
        position++;
        column++;

        if (position >= source.length()) {
            currentChar = '\0'; // End of file
        } else {
            currentChar = source.charAt(position);
        }
    }

    private void skipWhitespace() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) {
            if (currentChar == '\n') {
                line++;
                column = 0;
            }
            advance();
        }
    }

    private void skipComments() {
        if (currentChar == '/' && position + 1 < source.length()) {
            // Single-line comment
            if (source.charAt(position + 1) == '/') {
                while (currentChar != '\0' && currentChar != '\n') {
                    advance();
                }
                if (currentChar == '\n') {
                    line++;
                    column = 0;
                    advance();
                }
            }
            // Multi-line comment
            else if (source.charAt(position + 1) == '*') {
                advance(); // Skip '/'
                advance(); // Skip '*'

                boolean endComment = false;
                while (!endComment && currentChar != '\0') {
                    if (currentChar == '*' && position + 1 < source.length() &&
                        source.charAt(position + 1) == '/') {
                        advance(); // Skip '*'
                        advance(); // Skip '/'
                        endComment = true;
                    } else {
                        if (currentChar == '\n') {
                            line++;
                            column = 0;
                        }
                        advance();
                    }
                }
            }
        }
    }

    private Token number() {
        StringBuilder builder = new StringBuilder();
        int startColumn = column;
        boolean isFloat = false;

        while (currentChar != '\0' && (Character.isDigit(currentChar) || currentChar == '.')) {
            if (currentChar == '.') {
                if (isFloat) {
                    throw new RuntimeException("Invalid number format: multiple decimal points");
                }
                isFloat = true;
            }
            builder.append(currentChar);
            advance();
        }

        String lexeme = builder.toString();
        if (isFloat) {
            return new Token(TokenType.FLOAT_LITERAL, lexeme, line, startColumn);
        } else {
            return new Token(TokenType.INTEGER_LITERAL, lexeme, line, startColumn);
        }
    }

private Token identifier() {
    StringBuilder builder = new StringBuilder();
    int startColumn = column;

    while (currentChar != '\0' && 
          (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
        builder.append(currentChar);
        advance();
    }

    String lexeme = builder.toString();
    TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);

    return new Token(type, lexeme, line, startColumn);
}

    private Token stringLiteral() {
        StringBuilder builder = new StringBuilder();
        int startColumn = column;

        // Skip the opening quote
        advance();

        while (currentChar != '\0' && currentChar != '"') {
            // Handle escape sequences
            if (currentChar == '\\' && position + 1 < source.length()) {
                advance();
                switch (currentChar) {
                    case 'n': builder.append('\n'); break;
                    case 't': builder.append('\t'); break;
                    case 'r': builder.append('\r'); break;
                    case '\\': builder.append('\\'); break;
                    case '"': builder.append('"'); break;
                    default: builder.append('\\').append(currentChar);
                }
            } else {
                builder.append(currentChar);
            }
            advance();
        }

        if (currentChar != '"') {
            throw new RuntimeException("Unterminated string literal at line " + line);
        }

        // Skip the closing quote
        advance();

        return new Token(TokenType.STRING_LITERAL, builder.toString(), line, startColumn);
    }

    public Token getNextToken() {
        while (currentChar != '\0') {
            // Skip whitespace and comments
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
                continue;
            }

            if (currentChar == '/' && position + 1 < source.length() && 
                (source.charAt(position + 1) == '/' || source.charAt(position + 1) == '*')) {
                skipComments();
                continue;
            }

            // Identify tokens
            int startColumn = column;

            // Numbers
            if (Character.isDigit(currentChar)) {
                return number();
            }

            // Identifiers and keywords
            if (Character.isLetter(currentChar) || currentChar == '_') {
                return identifier();
            }

            // String literals
            if (currentChar == '"') {
                return stringLiteral();
            }

            // Operators and delimiters
            switch (currentChar) {
                case '[':
    advance();
    return new Token(TokenType.LEFT_BRACKET, "[", line, startColumn);
case ']':
    advance();
    return new Token(TokenType.RIGHT_BRACKET, "]", line, startColumn);
                case '+':
                    advance();
                    return new Token(TokenType.PLUS, "+", line, startColumn);
                case '-':
                    advance();
                    return new Token(TokenType.MINUS, "-", line, startColumn);
                case '*':
                    advance();
                    return new Token(TokenType.MULTIPLY, "*", line, startColumn);
                case '/':
                    advance();
                    return new Token(TokenType.DIVIDE, "/", line, startColumn);
                case '=':
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(TokenType.EQUAL, "==", line, startColumn);
                    } else {
                        return new Token(TokenType.ASSIGN, "=", line, startColumn);
                    }
                case '!':
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(TokenType.NOT_EQUAL, "!=", line, startColumn);
                    } else {
                        throw new RuntimeException("Expected '=' after '!' at line " + line + ", column " + startColumn);
                    }
                case '<':
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(TokenType.LTE, "<=", line, startColumn);
                    } else {
                        return new Token(TokenType.LT, "<", line, startColumn);
                    }
                case '>':
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Token(TokenType.GTE, ">=", line, startColumn);
                    } else {
                        return new Token(TokenType.GT, ">", line, startColumn);
                    }
                case ';':
                    advance();
                    return new Token(TokenType.SEMICOLON, ";", line, startColumn);
                case ',':
                    advance();
                    return new Token(TokenType.COMMA, ",", line, startColumn);
                case '(':
                    advance();
                    return new Token(TokenType.LEFT_PAREN, "(", line, startColumn);
                case ')':
                    advance();
                    return new Token(TokenType.RIGHT_PAREN, ")", line, startColumn);
                case '{':
                    advance();
                    return new Token(TokenType.LEFT_BRACE, "{", line, startColumn);
                case '}':
                    advance();
                    return new Token(TokenType.RIGHT_BRACE, "}", line, startColumn);
default:
    // Handle unexpected characters more gracefully
    char unexpected = currentChar;
    advance();
    throw new RuntimeException("Unexpected character: '" + unexpected + 
           "' at line " + line + ", column " + (startColumn));
            }
        }

        // End of file
        return new Token(TokenType.EOF, "", line, column);
    }

    // New tokenize method that returns a list of tokens
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = getNextToken();
            tokens.add(token);
        } while (token.getType() != TokenType.EOF);
        return tokens;
    }
}
