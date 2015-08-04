package de.ids_mannheim.korap.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import java.io.Reader;

public class KeywordAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents (final String fieldName,
            final Reader reader) {
        final Tokenizer source = new WhitespaceTokenizer(reader);
        TokenStream sink = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, sink);
    };
};
