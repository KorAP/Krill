package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import java.util.*;


public class SpanClassQueryWrapper implements SpanQueryWrapperInterface {
    private SpanQueryWrapperInterface subquery;
    private byte number = (byte) 0;

    public SpanClassQueryWrapper (SpanQueryWrapperInterface subquery, byte number) {
	this.subquery = subquery;
	this.number = number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapperInterface subquery, short number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapperInterface subquery, int number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapperInterface subquery) {
	this.subquery = subquery;
	this.number = (byte) 0;
    };

    public SpanQuery toQuery () {
	if (this.number == (byte) 0) {
	    return new SpanClassQuery((SpanQuery) this.subquery.toQuery());
	};
	return new SpanClassQuery((SpanQuery) this.subquery.toQuery(), (byte) this.number);
    };
};
