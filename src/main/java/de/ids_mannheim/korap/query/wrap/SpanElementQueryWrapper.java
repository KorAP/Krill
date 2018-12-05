package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

/*
 * SpanElementQuery has to support two constructors:
 * One respecting and adding node information to the payload,
 * one ignoring node information.
 * node aka depth in a tree information is only relevant for
 * child relation queries.
 */

public class SpanElementQueryWrapper extends SpanQueryWrapper {
    protected String element;
    String field;


    public SpanElementQueryWrapper (String field, String element) {
        this.field = field;
        this.element = element;
    };


    @Override
    public SpanQuery toFragmentQuery () throws QueryException {
        // Todo: Respect request for retrieving node data (i.e. depth information)
        return (SpanQuery) new SpanElementQuery(this.field, this.element);
    };


    @Override
    public boolean isNull () {
        return false;
    };
};
