package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.index.Term;

import java.util.*;

public class SpanWildcardQueryWrapper {
    private SpanQuery query;

    public SpanWildcardQueryWrapper (String field, String wc) {
	this(field, wc, false);
    };

    public SpanWildcardQueryWrapper (String field, String wc, boolean caseinsensitive) {
	if (caseinsensitive) {
	    if (wc.startsWith("s:")) {
		wc = wc.replaceFirst("s:", "i:");
	    };
	    wc = wc.toLowerCase();
	};
	WildcardQuery wcquery = new WildcardQuery(new Term(field, wc));
	query = new SpanMultiTermQueryWrapper<WildcardQuery>( wcquery );
    };

    public SpanQuery toQuery() {
	return this.query;
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return false;
    };

    public boolean isNegative () {
	return false;
    };
};
