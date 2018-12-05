package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.util.QueryException;

// Support multiple classes

public class SpanFocusQueryWrapper extends SpanQueryWrapper {
    private SpanQueryWrapper subquery;
    private byte number;


    public SpanFocusQueryWrapper (SpanQueryWrapper subquery, byte number) {
        this.subquery = subquery;
        this.number = number;
    };


    public SpanFocusQueryWrapper (SpanQueryWrapper subquery, short number) {
        this.subquery = subquery;
        this.number = (byte) number;
    };


    public SpanFocusQueryWrapper (SpanQueryWrapper subquery, int number) {
        this.subquery = subquery;
        this.number = (byte) number;
    };


    public SpanFocusQueryWrapper (SpanQueryWrapper subquery) {
        this.subquery = subquery;
        this.number = (byte) 1;
    };


    public SpanQuery toFragmentQuery () throws QueryException {
        if (this.subquery.isNull())
            return (SpanQuery) null;

        SpanFocusQuery sfq = new SpanFocusQuery(
            this.subquery.retrieveNode(this.retrieveNode).toFragmentQuery(),
            this.number);

        if (this.subquery.maybeUnsorted())
            sfq.setSorted(false);

        return sfq;
        
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
