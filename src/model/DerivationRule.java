package model;

import java.util.ArrayList;

/**
 * 推导规则
 */
public class DerivationRule {
    public Symbol left;
    public Expression right;

    public DerivationRule(Symbol left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof DerivationRule) {
            DerivationRule rule = (DerivationRule) another;
            return this.left.equals(rule.left) && this.right.equals(rule.right);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.left.hashCode() + this.right.hashCode();
    }

    @Override
    public String toString() {
        return right.equals(Expression.of(new ArrayList<Symbol>() {{ add(Symbol.EPSILON); }}))
                ? left + " -> ε"
                : left + " -> " + right;
    }
}