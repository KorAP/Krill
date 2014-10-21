package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.util.QueryException;

import de.ids_mannheim.korap.query.SpanMatchModifyClassQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import java.util.*;


public class SpanMatchModifyQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper subquery;
    private byte number;

    public SpanMatchModifyQueryWrapper (SpanQueryWrapper subquery, byte number) {
	this.subquery = subquery;
	this.number = number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapper subquery, short number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapper subquery, int number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapper subquery) {
	this.subquery = subquery;
	this.number = (byte) 0;
    };

    public SpanQuery toQuery () throws QueryException {
	if (this.subquery.isNull())
	    return (SpanQuery) null;
	return new SpanMatchModifyClassQuery(this.subquery.toQuery(), this.number);
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
