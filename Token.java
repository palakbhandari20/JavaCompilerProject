// Token.java
public class Token {
    public final String type;
    public final String value;
    public final int line;

    public Token(String type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    @Override
    public String toString() {
        return "Line " + line + ": <" + type + ", " + value + ">";
    }
}
