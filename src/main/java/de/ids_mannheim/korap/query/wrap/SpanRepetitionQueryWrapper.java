package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanRepetitionQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;


public class SpanRepetitionQueryWrapper implements SpanQueryWrapperInterface {
    private SpanQueryWrapperInterface subquery;
    private int min = 1;
    private int max = 1;

    public SpanRepetitionQueryWrapper (SpanQueryWrapperInterface subquery, int exact) {
	this.subquery = subquery;
	this.min = exact;
	this.max = exact;
    };

    public SpanRepetitionQueryWrapper (SpanQueryWrapperInterface subquery, int min, int max) {
	this.subquery = subquery;
	this.min = min;
	this.max = max;
    };

    public SpanQuery toQuery () {
	return new SpanRepetitionQuery(this.subquery.toQuery(), this.min, this.max, true);
    };
};
