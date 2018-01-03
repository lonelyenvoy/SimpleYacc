package model;

import java.util.List;
import java.util.Map;

/**
 * 语言
 * 用BNF文法和起始符号表示
 */
public class Language {
    public Map<Symbol, List<Expression>> bnfMap;
    public Symbol startSymbol;

    public Language() { }

    public Language(Map<Symbol, List<Expression>> bnfMap, Symbol startSymbol) {
        this.bnfMap = bnfMap;
        this.startSymbol = startSymbol;
    }
}