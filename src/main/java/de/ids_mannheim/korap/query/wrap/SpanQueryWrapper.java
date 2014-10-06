package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

// TODO: Add warning and error

/**
 * A wrapper class for Lucene Spanqueries that add certain information
 * to the queries, necessary for correct deserialization of nested queries.
 *
 * @author Nils Diewald
 */
public class SpanQueryWrapper {
    protected boolean isNull = true,
	              isOptional = false,
	              isNegative = false;
    protected int min = 1,
	          max = 1;

    // Serialize query to Lucene SpanQuery
    public SpanQuery toQuery () {
	return (SpanQuery) null;
    };

    // The subquery is not necessary, like in
    // "the [pos=ADJ]? tree"
    // The adjective can be there, but it's not necessary
    public boolean isOptional () {
	return this.isOptional;
    };

    // The subquery won't match anything at all,
    // like in
    // "the [pos=ADJ]{0} tree"
    public boolean isNull () {
	return this.isNull;
    };

    // The subquery should match if the condition does not hold true like in
    // "the [base!=tree]"
    public boolean isNegative () {
	return this.isNegative;
    };

    // Repetition queries may be more specific regarding repetition
    // This is a minimum repetition value
    public int min () {
	return this.min;
    };

    // Repetition queries may be more specific regarding repetition
    // This is a maximum repetition value
    public int max () {
	return this.max;
    };
};
