package de.ids_mannheim.korap.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import java.io.Reader;

/*
 * TODO:
 *   Prepend each term with a special marker like '~' and
 *   Prepend the tokenstream with a verbatim representation.
 *   That way it's possible to search by term verbatim and by text
 *   with a phrasequery!
 */

public class TextAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents (final String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream sink = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, sink);
    };
};
