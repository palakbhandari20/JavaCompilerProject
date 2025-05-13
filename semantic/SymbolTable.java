package semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    private Stack<Map<String, Symbol>> scopes;
    
    public SymbolTable() {
        scopes = new Stack<>();
        enterScope(); // Global scope
    }
    
    public void enterScope() {
        scopes.push(new HashMap<>());
    }
    
    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }
    
    public void define(Symbol symbol) {
        if (scopes.isEmpty()) {
            throw new RuntimeException("No active scope to define symbol in");
        }
        
        Map<String, Symbol> currentScope = scopes.peek();
        currentScope.put(symbol.getName(), symbol);
    }
    
    public Symbol resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Symbol> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        
        return null; // Symbol not found
    }
    
    public boolean isInGlobalScope() {
        return scopes.size() == 1;
    }
    
    public static class Symbol {
        private String name;
        private String type;  // Type name: "int", "float", "void", etc.
        private SymbolKind kind;
        
        public Symbol(String name, String type, SymbolKind kind) {
            this.name = name;
            this.type = type;
            this.kind = kind;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public SymbolKind getKind() {
            return kind;
        }
    }
    
    public enum SymbolKind {
        VARIABLE, FUNCTION, PARAMETER
    }
}