package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.util.QueryException;

import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;

import java.util.*;

// Support maybeUnsorted!
// Rename this to SpanFocusQueryWrapper
// Support multiple classes

// Sorting:
// - Sort with a buffer of matches, e.g. 25/50,
// So gather 50 hits, sort them, return the first 25,
// Add new 25, sort the last 50, return 25 etc.
// On processing, there should be an ability to raise
// a warning, in case an unordered result bubbles up.

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


    public SpanQuery toQuery () throws QueryException {
        if (this.subquery.isNull())
            return (SpanQuery) null;
        return new SpanFocusQuery(this.subquery.retrieveNode(this.retrieveNode)
                .toQuery(), this.number);
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
