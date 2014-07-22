package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanMatchModifyClassQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;

import java.util.*;


public class SpanMatchModifyQueryWrapper implements SpanQueryWrapperInterface {
    private SpanQueryWrapperInterface subquery;
    private byte number;

    public SpanMatchModifyQueryWrapper (SpanQueryWrapperInterface subquery, byte number) {
	this.subquery = subquery;
	this.number = number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapperInterface subquery, short number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapperInterface subquery, int number) {
	this.subquery = subquery;
	this.number = (byte) number;
    };

    public SpanMatchModifyQueryWrapper (SpanQueryWrapperInterface subquery) {
	this.subquery = subquery;
	this.number = (byte) 0;
    };

    public SpanQuery toQuery () {
	if (this.subquery.isNull())
	    return (SpanQuery) null;
	return new SpanMatchModifyClassQuery(this.subquery.toQuery(), this.number);
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return this.subquery.isNull();
    };
};
