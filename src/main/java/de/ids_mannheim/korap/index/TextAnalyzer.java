package de.ids_mannheim.korap.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import java.io.Reader;

public class TextAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents (final String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream sink = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, sink);
    };
};
