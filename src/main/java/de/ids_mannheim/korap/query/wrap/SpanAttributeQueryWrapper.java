package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.util.QueryException;

public class SpanAttributeQueryWrapper extends SpanQueryWrapper {

    boolean isNegation = false;
    private SpanQueryWrapper subquery;

    public SpanAttributeQueryWrapper (SpanQueryWrapper sqw, boolean inclusion) {
        this.subquery = sqw;
        if (!inclusion) {
            this.isNegation = true;
        };

        if (sqw.maybeUnsorted())
            this.maybeUnsorted = true;
    };

    @Override
    public SpanQuery toQuery() throws QueryException {

        SpanQuery sq = subquery.toQuery();
        if (sq instanceof SpanTermQuery) {
            return new SpanAttributeQuery((SpanTermQuery) sq, isNegation, true);
        }

        return null; // or exception??
    }
}
