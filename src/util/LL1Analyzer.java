package util;

import model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * LL1文法分析器
 */
public class LL1Analyzer {

    private Language language;
    private Set<Symbol> terminalSymbols;
    private Map<Symbol, Set<Symbol>> symbolFirstSetCache = new HashMap<>();
    private Map<Expression, Set<Symbol>> expressionFirstSetCache = new HashMap<>();
    private Map<Symbol, Set<Symbol>> followSetMap;

    private Map<PredictionAnalysisTableCellIndexer, DerivationRule> predictionAnalysisTable;

    private boolean isAnalyzed = false;
    private boolean analyzeResult;

    /**
     * 对象创建器（必须用此方法创建对象）
     * @param language 通过BNF解析器获得的语言
     * @return 文法分析器自身
     * @throws NotLL1GrammarException 当输入的文法不是LL1文法时抛出此异常
     */
    public static LL1Analyzer Builder(Language language) throws NotLL1GrammarException {
        LL1Analyzer analyzer = new LL1Analyzer();
        analyzer.language = language;
        analyzer.generatePredictionAnalysisTable();
        return analyzer;
    }

    /**
     * 取得终结符集合
     * @return 终结符集合
     */
    private Set<Symbol> getTerminalSymbols() {
        if (terminalSymbols != null) {
            return terminalSymbols;
        }
        terminalSymbols = new HashSet<>();
        Set<Symbol> keys = language.bnfMap.keySet();
        for (List<Expression> expressions : language.bnfMap.values()) {
            for (Expression expr : expressions) {
                for (Symbol symbol : expr.symbols) {
                    if (!keys.contains(symbol)) {
                        terminalSymbols.add(symbol);
                    }
                }
            }
        }
        return terminalSymbols;
    }

    /**
     * 取得文法符号的first集
     * @param symbol 要计算的文法符号
     * @return 该符号的first集
     */
    private Set<Symbol> getFirstSet(Symbol symbol) {
        if (symbolFirstSetCache.containsKey(symbol)) {
            return symbolFirstSetCache.get(symbol);
        }
        if (getTerminalSymbols().contains(symbol)) {
            return new HashSet<Symbol>() {{
                add(symbol);
            }};
        }
        Set<Symbol> firstSet = new HashSet<>();
        List<Expression> expressions = language.bnfMap.get(symbol);
        for (Expression expr : expressions) {
            boolean foundNonEpsilon = false;
            if (expr.symbols.equals(Symbol.EPSILON)) {
                firstSet.add(Symbol.EPSILON);
                continue;
            }
            for (int i = 0, size = expr.symbols.size(); i < size; i++) {
                Symbol currentSymbol = expr.symbols.get(i);
                Set<Symbol> currentSymbolFirst = getFirstSet(currentSymbol);
                if (!currentSymbolFirst.contains(Symbol.EPSILON)) {
                    firstSet.addAll(currentSymbolFirst);
                    foundNonEpsilon = true;
                    break; // 当前元素的first集中不包含epsilon，提前结束
                }
                if (!foundNonEpsilon) {
                    firstSet.addAll(currentSymbolFirst.stream()
                            .filter(x -> !x.equals(Symbol.EPSILON)).collect(Collectors.toSet()));
                }
                if (!currentSymbolFirst.contains(Symbol.EPSILON)) {
                    foundNonEpsilon = true;
                }
            }
            if (!foundNonEpsilon) {
                firstSet.add(Symbol.EPSILON);
            }
        }
        symbolFirstSetCache.put(symbol, firstSet);
        return firstSet;
    }

    /**
     * 取得表达式的first集
     * @param expression 要计算的表达式
     * @return 该表达式的first集
     */
    private Set<Symbol> getFirstSet(Expression expression) {
        if (expressionFirstSetCache.containsKey(expression)) {
            return expressionFirstSetCache.get(expression);
        }
        Set<Symbol> firstSet = new HashSet<>();
        boolean foundEpsilon = false;
        boolean continuousEpsilon = true;
        int index = 0;
        int size = expression.symbols.size();
        for (; index < size; ++index) {
            Set<Symbol> currentSymbolFirst = getFirstSet(expression.symbols.get(index));
            if (!currentSymbolFirst.contains(Symbol.EPSILON)) {
                continuousEpsilon = false;
            }
            for (Symbol symbolInCurrentSymbolFirst : currentSymbolFirst) {
                if (symbolInCurrentSymbolFirst.equals(Symbol.EPSILON)) {
                    foundEpsilon = true;
                } else {
                    firstSet.add(symbolInCurrentSymbolFirst);
                }
            }
            if (!foundEpsilon) {
                break;
            }
        }
        if (continuousEpsilon && index == size) {
            firstSet.add(Symbol.EPSILON);
        }
        expressionFirstSetCache.put(expression, firstSet);
        return firstSet;
    }

    /**
     * 计算文法所有非终结符号的follow集
     */
    private void calculateFollowSet() {
        followSetMap = new HashMap<Symbol, Set<Symbol>>() {{
            put(language.startSymbol, new HashSet<Symbol>() {{
                add(Symbol.of("$"));
            }});
        }};
        while (true) {
            boolean changed = false;
            for (Map.Entry<Symbol, List<Expression>> entry : language.bnfMap.entrySet()) {
                for (Expression expression : entry.getValue()) {
                    int size = expression.symbols.size();
                    for (int i = 0; i < size - 1; i++) {
                        Symbol currentSymbol = expression.symbols.get(i);
                        if (!getTerminalSymbols().contains(currentSymbol)) {
                            Set<Symbol> newFollowSet = Stream.concat(
                                    followSetMap.getOrDefault(currentSymbol, new HashSet<>()).stream(),
                                    getFirstSet(expression.symbols.get(i + 1)).stream()
                                            .filter(x -> !x.equals(Symbol.EPSILON))).collect(Collectors.toSet());
                            Set<Symbol> oldFollowSet = followSetMap.get(currentSymbol);
                            if (oldFollowSet == null || !oldFollowSet.equals(newFollowSet)) {
                                changed = true;
                                followSetMap.put(currentSymbol, newFollowSet);
                            }
                        }
                    }
                    for (int i = size - 1; i >= 0; i--) {
                        Symbol currentSymbol = expression.symbols.get(i);
                        Symbol nextSymbol = (i + 1 >= size) ? null : expression.symbols.get(i + 1);
                        if ((i + 1 >= size || getFirstSet(nextSymbol).contains(Symbol.EPSILON))) {
                            if (!getTerminalSymbols().contains(currentSymbol)
                                    && !getTerminalSymbols().contains(nextSymbol)) {
                                Set<Symbol> newFollowSet = Stream.concat(
                                        followSetMap.getOrDefault(currentSymbol, new HashSet<>()).stream(),
                                        followSetMap.getOrDefault(entry.getKey(), new HashSet<>()).stream())
                                        .collect(Collectors.toSet());
                                Set<Symbol> oldFollowSet = followSetMap.get(currentSymbol);
                                if (oldFollowSet == null || !oldFollowSet.equals(newFollowSet)) {
                                    changed = true;
                                    followSetMap.put(currentSymbol, newFollowSet);
                                }
                            }
                        } else {
                            break; // 不满足产生式右端符号first集中包含epsilon的条件，提前跳出循环
                        }
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    /**
     * 取得文法符号的follow集
     * @param symbol 要计算的文法符号
     * @return 该符号的follow集
     */
    private Set<Symbol> getFollowSet(Symbol symbol) {
        if (followSetMap == null) {
            calculateFollowSet();
        }
        return followSetMap.get(symbol);
    }

    /**
     * 生成预测分析表
     * @throws NotLL1GrammarException 当文法不是LL1文法时抛出此异常
     */
    private void generatePredictionAnalysisTable() throws NotLL1GrammarException {
        predictionAnalysisTable = new HashMap<>();
        for (Map.Entry<Symbol, List<Expression>> entry : language.bnfMap.entrySet()) {
            Symbol key = entry.getKey();
            for (Expression expr : entry.getValue()) {
                for (Symbol firstSetSymbol : getFirstSet(expr)) {
                    if (firstSetSymbol.equals(Symbol.EPSILON)) {
                        for (Symbol followSetSymbol : getFollowSet(key)) {
                            PredictionAnalysisTableCellIndexer indexer = new PredictionAnalysisTableCellIndexer(
                                    key, followSetSymbol
                            );
                            if (predictionAnalysisTable.containsKey(indexer)) {
                                throw new NotLL1GrammarException();
                            }
                            predictionAnalysisTable.put(indexer, new DerivationRule(key, expr));
                        }
                    } else if (getTerminalSymbols().contains(firstSetSymbol)) {
                        PredictionAnalysisTableCellIndexer indexer = new PredictionAnalysisTableCellIndexer(
                                key, firstSetSymbol
                        );
                        if (predictionAnalysisTable.containsKey(indexer)) {
                            throw new NotLL1GrammarException();
                        }
                        predictionAnalysisTable.put(indexer, new DerivationRule(key, expr));
                    }
                }
            }
        }
    }

    /**
     * 分析一个输入的单词流是否符合给定的文法
     * @param tokenFileName 单词流文件路径
     * @return 文法分析器自身
     * @throws IOException 找不到文件时抛出此异常
     */
    public LL1Analyzer analyze(String tokenFileName) throws IOException {
        isAnalyzed = true;
        List<String> tokens = Files.readAllLines(Paths.get(tokenFileName));
        if (tokens.isEmpty() || (tokens.size() == 1 && tokens.get(0).equals(""))) { // 空文件
            analyzeResult = false;
            return this;
        }
        tokens.add("$");
        Stack<Symbol> symbolStack = new Stack<Symbol>() {{
            push(Symbol.of("$"));
            push(language.startSymbol);
        }};

        int tokenIndex = 0;
        while (symbolStack.size() > 1) {
            Symbol topSymbol = symbolStack.peek();
            Symbol inputSymbol = Symbol.of(tokens.get(tokenIndex));
            PredictionAnalysisTableCellIndexer indexer = new PredictionAnalysisTableCellIndexer(
                    topSymbol, inputSymbol
            );
            if (topSymbol.equals(inputSymbol)) {
                // 匹配到终结符
                symbolStack.pop();
                tokenIndex++;
            } else if (getTerminalSymbols().contains(topSymbol)) {
                // 匹配失败 -- 输入符与栈顶不一致
                analyzeResult = false;
                return this;
            } else if (!predictionAnalysisTable.containsKey(indexer)) {
                // 匹配失败 -- 找不到表项
                analyzeResult = false;
                return this;
            } else {
                // 在栈顶用产生式右部替换左部
                DerivationRule rule = predictionAnalysisTable.get(indexer);
                System.out.println(rule);
                symbolStack.pop();
                for (int size = rule.right.symbols.size(), i = size - 1; i >= 0; i--) {
                    Symbol pushingSymbol = rule.right.symbols.get(i);
                    if (!pushingSymbol.equals(Symbol.EPSILON)) {
                        symbolStack.push(pushingSymbol);
                    }
                }
            }
        }
        analyzeResult = true;
        return this;
    }

    /**
     * 取得文法分析结果
     * @return 单词流是否符合给定的文法
     */
    public boolean result() {
        if (!isAnalyzed) {
            throw new IllegalStateException("Not yet analyzed!");
        }
        return analyzeResult;
    }

    /**
     * 异常类，表示给定的文法不是LL1文法
     */
    public class NotLL1GrammarException extends Exception {}
}

