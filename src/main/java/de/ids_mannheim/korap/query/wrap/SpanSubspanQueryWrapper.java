package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanSubspanQuery;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha
 * 
 */
public class SpanSubspanQueryWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper subquery;
    private int startOffset, length;

    private Logger log = LoggerFactory.getLogger(SpanSubspanQueryWrapper.class);

    public SpanSubspanQueryWrapper(SpanQueryWrapper sqw, int startOffset,
            int length) {
        this.subquery = sqw;
        this.startOffset = startOffset;
        this.length = length;
    }

    @Override
    public SpanQuery toQuery() throws QueryException {
        if (subquery == null) {
            log.warn("Subquery of SpanSubspanquery is null.");
            return null;
        }

        if (length == 0) {
            log.warn("Not SpanSubspanQuery. Creating only the subquery.");
            return subquery.toQuery();
        }

        SpanQuery sq = subquery.toQuery();
        if (sq instanceof SpanTermQuery) {
            log.warn("Not SpanSubspanQuery. Creating only the subquery.");
            return sq;
        }

        return new SpanSubspanQuery(subquery.toQuery(), startOffset, length,
                true);
    }
}
