package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.query.SpanSubspanQuery;
import de.ids_mannheim.korap.util.QueryException;

/**
 * @author margaretha, diewald
 * 
 */
public class SpanSubspanQueryWrapper extends SpanQueryWrapper {

    private SpanQueryWrapper subquery;
    private int startOffset, length;

    private final static Logger log =
        LoggerFactory.getLogger(SpanSubspanQueryWrapper.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public SpanSubspanQueryWrapper(SpanQueryWrapper sqw,
                                   int startOffset,
                                   int length) {
        this.subquery = sqw;
        if (sqw == null) {
            this.isNull = true;
            return;
        }
        else {
            this.isNull = false;
        };

        this.startOffset = startOffset;
        this.length = length;

        // The embedded class is empty,
        // but probably in a valid range
        // - optimize
        // subspan([]{,5}, 2) -> subspan([]{2,5}, 2)
        // subspan([]{2,}, 2,5) -> subspan([]{2,5}, 2,5)
        if (subquery.isEmpty()) {

            // Todo: Is there a possible way to deal with that?
            if (startOffset < 0) {
                this.isNull = true;
                return;
            };

            // e.g, subspan([]{0,6}, 8)
            if (subquery.getMax() < startOffset) {
                this.isNull = true;
                return;
            };

            // Readjust the minimum of the subquery
            if (startOffset > 0) {
                subquery.setMin(startOffset);
                subquery.isOptional = false;
            };

            // Readjust the maximum,
            // although the following case may be somehow disputable:
            // subspan([]{2,8}, 2, 1) -> subspan([]{2,5},2,1)
            if (length > 0) {
                int newMax = subquery.getMin() + startOffset + length;
                if (subquery.getMax() > newMax) {
                    subquery.setMax(subquery.getMin() + length);
                };
            };
        };

        // Todo: What happens with negative queries?
        // submatch([base!=tree],3)
    }

    @Override
    public SpanQuery toQuery() throws QueryException {

        if (this.isNull() || subquery.isNull()) {
            if (DEBUG)
                log.warn("Subquery of SpanSubspanquery is null.");
            return null;
        };

        if (startOffset == 0 && length == 0) {
            if (DEBUG)
                log.warn("Not SpanSubspanQuery. Creating only the subquery.");
            return subquery.toQuery();
        };

        // The embedded subquery may be null
        SpanQuery sq = subquery.toQuery();
        if (sq == null) return null;
        
        if (sq instanceof SpanTermQuery) {

            // No relevant subspan
            if ((startOffset == 0 || startOffset == -1) &&
                (length <= 1)) {
                if (DEBUG)
                    log.warn("Not SpanSubspanQuery. " +
                             "Creating only the subquery.");
                return sq;
            };

            // Subspanquery can't match (always out of scope)
            return null;
        }

        return new SpanSubspanQuery(sq, startOffset, length,
                true);
    }

    @Override
    public boolean isNegative () {
        return this.subquery.isNegative();
    };

    @Override
    public boolean isOptional () {
        if (startOffset > 0)
            return false;
        return this.subquery.isOptional();
    };
}
