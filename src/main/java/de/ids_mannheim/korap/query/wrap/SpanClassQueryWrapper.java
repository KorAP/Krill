package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

import java.util.*;


public class SpanClassQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper subquery;
    private byte number = (byte) 0;

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, byte number) {
	this.subquery = subquery;
	this.number = number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, short number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery, int number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanClassQueryWrapper (SpanQueryWrapper subquery) {
	this.subquery = subquery;
	this.number = (byte) 0;
    };

    public SpanQuery toQuery () throws QueryException {
	if (this.subquery.isNull())
	    return (SpanQuery) null;

	// TODO: If this.subquery.isNegative(), it may be an Expansion!
	// SpanExpansionQuery(x, y.negative, min, max. direction???, classNumber, true)
	
	if (this.number == (byte) 0) {
	    return new SpanClassQuery((SpanQuery) this.subquery.toQuery());
	};
	return new SpanClassQuery((SpanQuery) this.subquery.toQuery(), (byte) this.number);
    };

    public boolean isOptional () {
	return this.subquery.isOptional();
    };

    public boolean isNull () {
	return this.subquery.isNull();
    };

    public boolean isNegative () {
	return this.subquery.isNegative();
    };
};
