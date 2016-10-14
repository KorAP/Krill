package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// DEAL WITH NEGATIVITY

public class SpanRepetitionQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper subquery;

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(SpanSequenceQueryWrapper.class);


    public SpanRepetitionQueryWrapper () {
        this.isEmpty = true;
        this.isNull = false;
    };


    // This is for exact enumbered repetition, like in a{3}
    public SpanRepetitionQueryWrapper (SpanQueryWrapper subquery, int exact) {
        if (!subquery.isEmpty()) {
            this.subquery = subquery;
            if (subquery.maybeUnsorted())
                this.maybeUnsorted = true;
        }
        else
            this.isEmpty = true;

        if (exact < 1 || this.subquery.isNull()) {
            this.isNull = true;
            this.isOptional = true;
            this.min = 0;
            this.max = 0;
            return;
        };

        this.min = exact;
        this.max = exact;
    };


    // This is for a range of repetitions, like in a{2,3}, a{,4}, a{3,}, a+, a*, a?
    public SpanRepetitionQueryWrapper (SpanQueryWrapper subquery, int min,
                                       int max) {

        if (!subquery.isEmpty()) {
            this.subquery = subquery;

            if (subquery.maybeUnsorted())
                this.maybeUnsorted = true;
        }
        else
            this.isEmpty = true;

        // Subquery may be an empty token
        if (subquery.isNull()) {
            this.isNull = true;
            return;
        }
        else {
            this.isNull = false;
        };

        if (min == 0) {
            this.isOptional = true;
            min = 1;
            if (max == 0)
                this.isNull = true;
        };

        this.min = min;
        this.max = max;
    };


    // Serialize to Lucene SpanQuery
    @Override
    public SpanQuery toFragmentQuery () throws QueryException {

        // The query is null
        if (this.isNull)
            return (SpanQuery) null;

        if (this.isEmpty) {
            log.error("You can't queryize an empty query");
            return (SpanQuery) null;
        };

        // The query is not a repetition query at all, but may be optional
        if (this.min == 1 && this.max == 1)
            return this.subquery.retrieveNode(this.retrieveNode)
                    .toFragmentQuery();

        // That's a fine repetition query
        return new SpanRepetitionQuery(
                this.subquery.retrieveNode(this.retrieveNode).toFragmentQuery(),
                this.min, this.max, true);
    };


    public boolean isNegative () {
        if (this.subquery == null)
            return false;
        return this.subquery.isNegative();
    };
};
