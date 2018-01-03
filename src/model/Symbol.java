package model;

/**
 * 文法符号
 */
public class Symbol {
    public String content;
    public static final Symbol EPSILON = Symbol.of("");

    public static Symbol of(String content) {
        Symbol symbol = new Symbol();
        symbol.content = content;
        return symbol;
    }

    public Symbol concat(Symbol another) {
        Symbol symbol = new Symbol();
        symbol.content = this.content.concat(another.content);
        return symbol;
    }

    @Override
    public boolean equals(Object another) {
        return another instanceof Symbol && this.content.equals(((Symbol) another).content);
    }

    @Override
    public int hashCode() {
        return this.content.hashCode();
    }

    @Override
    public String toString() {
        return content.equals("") ? "ε" : content;
    }
}

