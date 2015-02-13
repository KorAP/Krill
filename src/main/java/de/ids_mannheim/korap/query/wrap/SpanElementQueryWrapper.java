package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

public class SpanElementQueryWrapper extends SpanQueryWrapper {
    String element;
    String field;

    public SpanElementQueryWrapper (String field, String element) {
        this.field = field;
        this.element = element;
    };

    public SpanQuery toQuery () throws QueryException {
        return (SpanQuery) new SpanElementQuery(this.field, this.element);
    };

    public boolean isNull () {
        return false;
    };
};
