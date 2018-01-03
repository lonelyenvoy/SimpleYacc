package util;

import model.Expression;
import model.Language;
import model.Symbol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * BNF语法定义解析器
 */
public class BNFProcessor {

    private Language language = new Language();

    private BNFProcessor() { }

    /**
     * 对象创建器（必须用此方法创建对象）
     * @return 一个新的语法解析器
     */
    public static BNFProcessor Builder() {
        return new BNFProcessor();
    }

    /**
     * 解析BNF定义的文法
     * @param bnfFileName BNF文件路径
     * @return 语法解析器自身
     * @throws IOException 找不到文件时抛出IOException
     */
    public BNFProcessor parseBNF(String bnfFileName) throws IOException {
        language.bnfMap = new HashMap<>();
        String bnf = new String(Files.readAllBytes(Paths.get(bnfFileName)));
        Pattern derivationRulePattern = Pattern.compile("\\s*(<.+>)\\s*::=\\s*(.+)\\s*");
        Pattern elementPattern = Pattern.compile("<.+?>|\".*?\"");
        Matcher derivationRuleMatcher = derivationRulePattern.matcher(bnf);
        boolean firstLine = true;
        while (derivationRuleMatcher.find()) {
            String left = derivationRuleMatcher.group(1);
            String[] right = derivationRuleMatcher.group(2).split("\\|");

            List<Expression> expressions = new ArrayList<>();
            for (String singleExpression : right) {
                Matcher elementMatcher = elementPattern.matcher(singleExpression);
                Expression expression = new Expression();
                while (elementMatcher.find()) {
                    String element = elementMatcher.group();
                    if (element.equals("\"\"")) element = ""; // 空串
                    expression.symbols.add(Symbol.of(element));
                }
                expressions.add(expression);
            }
            Symbol key = Symbol.of(left);
            if (firstLine) {
                language.startSymbol = key;
                firstLine = false;
            }
            if (language.bnfMap.containsKey(key)) {
                language.bnfMap.put(key, Stream.concat(
                        language.bnfMap.get(Symbol.of(left)).stream(),
                        expressions.stream())
                        .collect(Collectors.toList()));
            } else {
                language.bnfMap.put(key, expressions);
            }
        }
        return this;
    }

    /**
     * 消除文法的左递归
     * @return 语法解析器自身
     */
    public BNFProcessor eliminateLeftRecursion() {
        // 获取Entry，用于遍历BNF数据结构
        List<Map.Entry<Symbol, List<Expression>>> bnfEntryList = new ArrayList<>(language.bnfMap.entrySet());

        // 为产生式中的字符编号
        Map<Symbol, Integer> symbolNumberMap = new HashMap<>();
        Map<Integer, Symbol> numberSymbolMap = new HashMap<>();

        int numberOfSymbols = 0;
        for (int i = 0, size = bnfEntryList.size(); i < size; i++) {
            Map.Entry<Symbol, List<Expression>> entry = bnfEntryList.get(i);
            Symbol key = entry.getKey();
            if (!symbolNumberMap.containsKey(key)) {
                ++numberOfSymbols;
                symbolNumberMap.put(key, numberOfSymbols);
                numberSymbolMap.put(numberOfSymbols, key);
            }
            for (Expression expr : entry.getValue()) {
                for (Symbol symbol : expr.symbols) {
                    if (!symbolNumberMap.containsKey(symbol)) {
                        ++numberOfSymbols;
                        symbolNumberMap.put(symbol, numberOfSymbols);
                        numberSymbolMap.put(numberOfSymbols, symbol);
                    }
                }
            }
        }

        // 消除左递归
        for (int i = 1; i <= numberOfSymbols; i++) {
            bnfEntryList = new ArrayList<>(language.bnfMap.entrySet());

            Map<Symbol, Integer> symbolInLeftOfNthBNFExpressionMap = new HashMap<>();
            Map<Symbol, List<Integer>> symbolInRightOfNthBNFExpressionMap = new HashMap<>();
            for (int bnfEntryCount = 0, size = bnfEntryList.size(); bnfEntryCount < size; bnfEntryCount++) {
                Map.Entry<Symbol, List<Expression>> entry = bnfEntryList.get(bnfEntryCount);
                Symbol key = entry.getKey();
                symbolInLeftOfNthBNFExpressionMap.put(key, bnfEntryCount + 1);

                for (Expression expr : entry.getValue()) {
                    for (Symbol symbol : expr.symbols) {
                        if (symbolInRightOfNthBNFExpressionMap.containsKey(symbol)) {
                            symbolInRightOfNthBNFExpressionMap.get(symbol).add(bnfEntryCount + 1);
                        } else {
                            List<Integer> integerList = new ArrayList<>();
                            integerList.add(bnfEntryCount + 1);
                            symbolInRightOfNthBNFExpressionMap.put(symbol, integerList);
                        }
                    }
                }
            }

            Symbol ithSymbol = numberSymbolMap.get(i);
            Integer targetLeftExpressionIndex = symbolInLeftOfNthBNFExpressionMap.get(ithSymbol);
            if (targetLeftExpressionIndex == null) { // 左边没有此符号，跳过
                continue;
            }
            boolean changed = false;
            for (int j = 1; j < i; j++) {
                if (changed) {
                    bnfEntryList = new ArrayList<>(language.bnfMap.entrySet());

                    symbolInLeftOfNthBNFExpressionMap = new HashMap<>();
                    symbolInRightOfNthBNFExpressionMap = new HashMap<>();
                    for (int bnfEntryCount = 0, size = bnfEntryList.size(); bnfEntryCount < size; bnfEntryCount++) {
                        Map.Entry<Symbol, List<Expression>> entry = bnfEntryList.get(bnfEntryCount);
                        Symbol key = entry.getKey();
                        symbolInLeftOfNthBNFExpressionMap.put(key, bnfEntryCount + 1);

                        for (Expression expr : entry.getValue()) {
                            for (Symbol symbol : expr.symbols) {
                                if (symbolInRightOfNthBNFExpressionMap.containsKey(symbol)) {
                                    symbolInRightOfNthBNFExpressionMap.get(symbol).add(bnfEntryCount + 1);
                                } else {
                                    List<Integer> integerList = new ArrayList<>();
                                    integerList.add(bnfEntryCount + 1);
                                    symbolInRightOfNthBNFExpressionMap.put(symbol, integerList);
                                }
                            }
                        }
                    }
                }
                changed = false;
                Symbol jthSymbol = numberSymbolMap.get(j);
                if (!symbolInLeftOfNthBNFExpressionMap.containsKey(jthSymbol)) { // 不是非终结符，跳过
                    continue;
                }
                List<Integer> targetRightExpressionIndexes = symbolInRightOfNthBNFExpressionMap.get(jthSymbol);
                if (targetRightExpressionIndexes == null) {
                    continue;
                }
                for (Integer targetRightExpressionIndex : targetRightExpressionIndexes) {
                    Map.Entry<Symbol, List<Expression>> targetRightEntry = bnfEntryList.get(targetRightExpressionIndex - 1);
                    if (symbolInLeftOfNthBNFExpressionMap.get(targetRightEntry.getKey()).equals(targetLeftExpressionIndex)) {
//                    if (targetLeftExpressionIndex.equals(targetRightExpressionIndex)) {
//                        int iTargetIndex = targetLeftExpressionIndex;
//                        int jTargetIndex = symbolInLeftOfNthBNFExpressionMap.get(jthSymbol);
                        Map.Entry<Symbol, List<Expression>> ithSymbolEntry = bnfEntryList.get(targetLeftExpressionIndex - 1);
                        List<Expression> irrelevantExpressions = new ArrayList<>();
                        for (Expression expr : ithSymbolEntry.getValue()) {
                            if (!expr.symbols.isEmpty()) {
                                Symbol firstSymbol = expr.symbols.get(0);
                                if (!symbolNumberMap.get(firstSymbol).equals(j)) {
                                    irrelevantExpressions.add(expr);
                                }
                            }
                        }
                        List<Expression> newExpressions = new ArrayList<>();
                        for (Expression expr : ithSymbolEntry.getValue()) {
                            if (!expr.symbols.isEmpty()) {
                                Symbol firstSymbol = expr.symbols.get(0);
                                if (symbolNumberMap.get(firstSymbol).equals(j)) {
                                    List<Symbol> leftoverSymbols = expr.symbols.subList(1, expr.symbols.size());
                                    List<Expression> jthSymbolExpressions = language.bnfMap.get(jthSymbol);
                                    if (jthSymbolExpressions == null) {
                                        continue;
                                    }
                                    for (Expression jthSymbolExpr : jthSymbolExpressions) {
                                        List<Symbol> newSymbols = new ArrayList<>(jthSymbolExpr.symbols);
                                        newSymbols.addAll(leftoverSymbols);
                                        newExpressions.add(Expression.of(newSymbols));
                                    }
                                }
                            }
                        }
                        language.bnfMap.put(ithSymbolEntry.getKey(), Stream.concat(
                                newExpressions.stream(), irrelevantExpressions.stream()).collect(Collectors.toList()));
                        changed = true;
                    }
                }
            }

            // 找出第i个符号的立即左递归产生式
            Map.Entry<Symbol, List<Expression>> entry = bnfEntryList.get(targetLeftExpressionIndex - 1);
            List<Expression> expressionsWithLeftRecursion = new ArrayList<>();
            List<Expression> expressionsWithoutLeftRecursion = new ArrayList<>();
            for (Expression expr : entry.getValue()) {
                if (!expr.symbols.isEmpty() && expr.symbols.get(0).equals(entry.getKey())) { // 发现立即左递归
                    expressionsWithLeftRecursion.add(expr);
                } else {
                    expressionsWithoutLeftRecursion.add(expr);
                }
            }
            if (expressionsWithLeftRecursion.isEmpty()) {
                continue; // 没有立即左递归，跳过本轮循环
            }
            // 消除立即左递归
            List<Expression> newExpressionsFirst = new ArrayList<>();
            List<Expression> newExpressionsSecond = new ArrayList<>();
            // 生成一个新的随机Symbol，用于表示消除左递归后第二层产生式左边的符号
            Symbol secondSymbol = Symbol.of(entry.getKey().content.concat("-").concat(UUID.randomUUID().toString()));
            for (Expression exprWithoutLeftRecursion : expressionsWithoutLeftRecursion) {
                newExpressionsFirst.add(exprWithoutLeftRecursion.concat(secondSymbol));
            }
            for (Expression exprWithLeftRecursion : expressionsWithLeftRecursion) {
                List<Symbol> leftoverSymbols
                        = exprWithLeftRecursion.symbols.subList(1, exprWithLeftRecursion.symbols.size());
                leftoverSymbols.add(secondSymbol);
                newExpressionsSecond.add(Expression.of(leftoverSymbols));
            }
            language.bnfMap.put(entry.getKey(), newExpressionsFirst);
            // 加入空串
            newExpressionsSecond.add(Expression.of(new ArrayList<Symbol>(){{ add(Symbol.EPSILON); }}));
            language.bnfMap.put(secondSymbol, newExpressionsSecond);
        }
        return this;
    }

    /**
     * 取得解析结果
     * @return 解析后的语言
     */
    public Language result() {
        return language;
    }
}