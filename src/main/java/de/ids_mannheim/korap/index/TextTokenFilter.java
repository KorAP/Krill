package de.ids_mannheim.korap.index;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/*
 * THIS IS PROBABLY USELESS
 */

public final class TextTokenFilter extends TokenFilter {
    private Boolean initTerm;
    private static String verbatim;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);


    public TextTokenFilter (TokenStream in) {
        super(in);
        this.initTerm = true;
    }


    @Override
    public final boolean incrementToken () throws IOException {

        // Prepend verbatim string
        if (this.initTerm) {
            clearAttributes();
            termAtt.append("[PREPEND2]");
            posIncrAtt.setPositionIncrement(10000);
            this.initTerm = false;
            return true;
        };

        // IncrementToken
        if (input.incrementToken()) {
            return true;
        };

        return false;
    }
};
