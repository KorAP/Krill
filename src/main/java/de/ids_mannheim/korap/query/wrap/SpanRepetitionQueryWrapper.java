package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;


public class SpanRepetitionQueryWrapper implements SpanQueryWrapperInterface {
    private SpanQueryWrapperInterface subquery;
    private int min = 1;
    private int max = 1;
    private boolean optional = false;
    private boolean isNull = false;

    public SpanRepetitionQueryWrapper (SpanQueryWrapperInterface subquery, int exact) {
	this.subquery = subquery;

	if (exact < 1 || this.subquery.isNull()) {
	    this.isNull = true;
	    return;
	};
	
	this.min = exact;
	this.max = exact;
    };

    public SpanRepetitionQueryWrapper (SpanQueryWrapperInterface subquery, int min, int max) {
	this.subquery = subquery;

	if (this.subquery.isNull()) {
	    this.isNull = true;
	    return;
	};
	
	if (min == 0) {
	    this.optional = true;
	    if (max == 0)
		this.isNull = true;
	};
	this.min = min;
	this.max = max;
    };

    public SpanQuery toQuery () {
	if (this.isNull)
	    return (SpanQuery) null;
	return new SpanRepetitionQuery(this.subquery.toQuery(), this.min, this.max, true);
    };

    public boolean isOptional () {
	return this.optional;
    };

    public boolean isNull () {
	return this.isNull;
    };
};
