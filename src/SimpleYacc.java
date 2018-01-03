
import model.Language;
import util.BNFProcessor;
import util.LL1Analyzer;

import java.io.IOException;

public class SimpleYacc {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: java " + SimpleYacc.class.getSimpleName() + " <BNF_GRAMMAR_FILE_NAME> <TEST_CASE_TOKENS_FILE_NAME>");
            return;
        }
        String bnfGrammarFileName = args[0];
        String testCaseTokensFileName = args[1];
        try {
            Language language = BNFProcessor
                    .Builder()
                    .parseBNF(bnfGrammarFileName)
                    .eliminateLeftRecursion()
                    .result();
            boolean analysisResult = LL1Analyzer
                    .Builder(language)
                    .analyze(testCaseTokensFileName)
                    .result();
            System.out.println(analysisResult ? "YES" : "NO");
        } catch (LL1Analyzer.NotLL1GrammarException e) {
            System.err.println("错误：BNF文件中定义的文法不是LL1文法，无法解析。");
        } catch (IOException e) {
            System.err.println("错误：无法读取文件，请检查文件路径是否正确。");
        }
    }
}

