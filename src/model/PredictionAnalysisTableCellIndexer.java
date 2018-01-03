package model;

/**
 * 预测分析表索引
 */
public class PredictionAnalysisTableCellIndexer {
    public Symbol nonTerminalSymbol;
    public Symbol inputSymbol;

    public PredictionAnalysisTableCellIndexer(Symbol nonTerminalSymbol, Symbol inputSymbol) {
        this.nonTerminalSymbol = nonTerminalSymbol;
        this.inputSymbol = inputSymbol;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof PredictionAnalysisTableCellIndexer) {
            PredictionAnalysisTableCellIndexer indexer = (PredictionAnalysisTableCellIndexer) another;
            return this.inputSymbol.equals(indexer.inputSymbol)
                    && this.nonTerminalSymbol.equals(indexer.nonTerminalSymbol);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.inputSymbol.hashCode() * 2 + this.nonTerminalSymbol.hashCode();
    }

    @Override
    public String toString() {
        return "[" + nonTerminalSymbol + ", " + inputSymbol + "]";
    }
}
