package lexer;

import javax.print.DocFlavor.STRING;

public enum TokenType {
    // Literals
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    IDENTIFIER,
    LEFT_BRACKET,   // [
    RIGHT_BRACKET,  // ]
    PUBLIC,
    STATIC,

    // Keywords
    INT,
    FLOAT,
    IF,
    ELSE,
    WHILE,
    RETURN,
    VOID,
    STRING,
    BOOLEAN,
    CLASS,

    // Operators
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    ASSIGN,
    EQUAL,
    NOT_EQUAL,
    LT,  // Less Than
    LTE, // Less Than or Equal
    GT,  // Greater Than
    GTE, // Greater Than or Equal

    // Punctuation and Delimiters
    SEMICOLON,
    COMMA,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,

    // Special
    EOF // End of File
}