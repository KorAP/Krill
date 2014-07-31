package de.ids_mannheim.korap.query.wrap;

import java.util.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

import de.ids_mannheim.korap.query.wrap.SpanQueryWrapperInterface;
import org.apache.lucene.search.spans.SpanQuery;

// This might be irrelevant now with repetition!

public class SpanQuantifierQueryWrapper implements SpanQueryWrapperInterface {
    private String field;

    public SpanQuantifierQueryWrapper (String field) {
	this.field = field;
    };

    public SpanQuery toQuery () {
	return (SpanQuery) null;
    };

    public boolean isOptional () {
	return false;
    };

    public boolean isNull () {
	return false;
    };

    public boolean isNegative () {
	return false;
    };


    /*

Only support spans with minimal one occurrence and then
flag spans with NOT_NECESSARY.
This unfortunately means to support this in at least spanNextQuery
Problem: Queries without context:

[]*[s:tree]? -> matches everything!

The any segment is special, it shuld be supported by a special
spanNextQuery, where it adds a position (or more) to the matching span.
spanNext(Query1, ANY)

      API idea:
      opt();
      star();
      plus();
      occ(2);
      occ(2, this.UNLIMITED);
      occ(0, 4);
      occ(5, 8);

      Implementation idea:
      This query should work similar to NextSpans with looking at all matching spans
      in order per document, returning matching positions for all sequences in the boundary.
      All actions should be translated to {x,y} boundaries.
      ?     -> {0,1}
      +     -> {1,UNL}
      *     -> {0,UNL}
      (2)   -> {2,2}
      (,3)  -> {0,3}
      (3,)  -> {3,UNL}
      (3,4) -> {3,4}

      oldSpanEnd = X;
      For (i = 0; i < orderedSpans.length; i) {
      # ...
      };

    */
};

