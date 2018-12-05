package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanQuery;

public class SpanWildcardQueryWrapper extends SpanQueryWrapper {
    private SpanQuery query;


    public SpanWildcardQueryWrapper (String field, String wc) {
        this(field, wc, false);
    };


    public SpanWildcardQueryWrapper (String field, String wc,
                                     boolean caseinsensitive) {
        if (caseinsensitive) {
            if (wc.startsWith("s:")) {
                wc = wc.replaceFirst("s:", "i:");
            };
            wc = wc.toLowerCase();
        };
        WildcardQuery wcquery = new WildcardQuery(new Term(field, wc));
        query = new SpanMultiTermQueryWrapper<WildcardQuery>(wcquery);
    };


    public SpanQuery toFragmentQuery () {
        return this.query;
    };


    public boolean isNull () {
        return false;
    };
};
