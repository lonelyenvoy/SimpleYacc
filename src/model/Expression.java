package model;

import java.util.LinkedList;
import java.util.List;

/**
 * 表达式
 */
public class Expression {
    public List<Symbol> symbols = new LinkedList<>(); // 用LinkedList提高性能

    public static Expression of(List<Symbol> symbols) {
        Expression expression = new Expression();
        expression.symbols = symbols;
        return expression;
    }

    public Expression concat(Symbol symbol) {
        Expression expression = new Expression();
        List<Symbol> newSymbols = new LinkedList<>(this.symbols);
        newSymbols.add(symbol);
        expression.symbols = newSymbols;
        return expression;
    }

    @Override
    public boolean equals(Object another) {
        return another instanceof Expression && this.symbols.equals(((Expression) another).symbols);
    }

    @Override
    public int hashCode() {
        return this.symbols.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Symbol symbol : symbols) {
            if (first) {
                first = false;
            } else {
                builder.append(" ");
            }
            builder.append(symbol.content);
        }
        return builder.toString();
    }
}