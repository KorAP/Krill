package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha
 * */
public class SpanAttributeQueryWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper subquery;


    public SpanAttributeQueryWrapper (SpanQueryWrapper sqw) {
        if (sqw == null) {
            isNull = true;
            return;
        }
        if (sqw.isEmpty()) {
            isEmpty = true;
            return;
        }

        this.subquery = sqw;
        if (sqw.isNegative) {
            this.isNegative = true;
        };

        if (sqw.maybeUnsorted())
            this.maybeUnsorted = true;
    };


    @Override
    public SpanQuery toQuery () throws QueryException {
        if (isNull || isEmpty)
            return null;

        SpanQuery sq = subquery.retrieveNode(this.retrieveNode).toQuery();
        if (sq == null) {
            isNull = true;
            return null;
        }

        if (sq instanceof SpanTermQuery) {
            return new SpanAttributeQuery((SpanTermQuery) sq, isNegative, true);
        }
        else {
            throw new IllegalArgumentException(
                    "The subquery is not a SpanTermQuery.");
        }
    }
}
