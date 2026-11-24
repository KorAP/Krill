package de.ids_mannheim.korap.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableFieldType;

/**
 * A field with a stored string value that is indexed with a given
 * token stream instead of an analyzer.
 *
 * Up to Lucene 9, <tt>Field.setTokenStream()</tt> added a token
 * stream to a field with a stored string value. Since Lucene 10, a
 * field holds either the string or the token stream, so the token
 * stream is kept here.
 */
public class StoredTokenStreamField extends Field {

    private final TokenStream tokenStream;


    public StoredTokenStreamField (String name, String value,
                                   TokenStream tokenStream,
                                   IndexableFieldType type) {
        super(name, value, type);
        this.tokenStream = tokenStream;
    };


    @Override
    public TokenStream tokenStreamValue () {
        return this.tokenStream;
    };


    @Override
    public TokenStream tokenStream (Analyzer analyzer, TokenStream reuse) {
        return this.tokenStream;
    };
};
