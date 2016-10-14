package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanSimpleQueryWrapper extends SpanQueryWrapper {
    private SpanQuery query;


    public SpanSimpleQueryWrapper (String field, String term) {
        this.isNull = false;
        this.query = new SpanTermQuery(new Term(field, term));
    };


    public SpanSimpleQueryWrapper (String field, String term, boolean value) {
        this(field, term);
        this.isNegative = !value;
    }


    public SpanSimpleQueryWrapper (SpanQuery query) {
        this.isNull = false;
        this.query = query;
    };


    // This is similar to a clone
    public SpanSimpleQueryWrapper (SpanQueryWrapper query)
            throws QueryException {
        this.hasClass = query.hasClass();
        this.isOptional = query.isOptional();
        this.isNegative = query.isNegative();
        this.isEmpty = query.isEmpty();
        this.isExtended = query.isExtended();
        this.isExtendedToTheRight = query.isExtendedToTheRight();
        this.maybeUnsorted = query.maybeUnsorted();
        this.retrieveNode = query.retrieveNode;
        this.query = query.toFragmentQuery();
        this.isNull = query.isNull();
    };


    @Override
    public SpanQuery toFragmentQuery () {
        return this.query;
    };
};
