package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;
import de.ids_mannheim.korap.util.QueryException;

// TODO: Add warning and error

/**
 * A wrapper class for Lucene Spanqueries that add certain information
 * to the queries, necessary for correct deserialization of nested queries.
 *
 * @author Nils Diewald
 */
public class SpanQueryWrapper {
    protected int min = 1,
	          max = 1;

    protected byte number = (byte) 0;
    protected boolean hasClass = false;

    protected boolean isNull = true,
	              isOptional = false,
 	              isNegative = false,
 	              isEmpty = false,
	              isExtendedToTheRight;

    // Serialize query to Lucene SpanQuery
    public SpanQuery toQuery () throws QueryException {
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
	if (this.getMin() == 0 && this.getMax() == 0)
	    return true;
	return this.isNull;
    };

    // The subquery should match if the condition does not hold true like in
    // "the [base!=tree]"
    public boolean isNegative () {
	return this.isNegative;
    };

    // The subquery should match everything, like in
    // "the []"
    public boolean isEmpty () {
	return this.isEmpty;
    };

    // The subquery may exceed the right text offset due to an empty extension
    // [base=tree][]{3,4}
    // This makes it necessary to check the last position of the span
    // for match testing
    public boolean isExtendedToTheRight () {
	return this.isExtendedToTheRight;
    };


    // Check, if the query may be an anchor
    // in a SpanSequenceQueryWrapper
    public boolean maybeAnchor () {
	if (this.isNegative())
	    return false;

	if (this.isOptional())
	    return false;

	if (this.isEmpty())
	    return false;

	return true;
    };

    // Oposite to maybeAnchor - means "it is complicated"
    public boolean maybeExtension () {
	return !this.maybeAnchor();
    };

    // Repetition queries may be more specific regarding repetition
    // Get minimum repetition value
    public int getMin () {
	return this.min;
    };

    // Repetition queries may be more specific regarding repetition
    // Get maximum repetition value
    public int getMax () {
	return this.max;
    };

    // Set minimum repetition value
    public SpanQueryWrapper setMin (int min) {
	this.min = min;
	return this;
    };

    // Set maximum repetition value
    public SpanQueryWrapper setMax (int max) {
	this.max = max;
	return this;
    };


    // Empty tokens may have class information
    public boolean hasClass () {
	return this.hasClass;
    };

    public SpanQueryWrapper hasClass (boolean value) {
	this.hasClass = value;
	return this;
    };

    // Get class number
    public byte getClassNumber () {
	return this.number;
    };

    // Set class number
    public SpanQueryWrapper setClassNumber (byte number) {
	this.hasClass = true;
	this.number = number;
	return this;
    };

    // Set class number
    public SpanQueryWrapper setClassNumber (short number) {
	return this.setClassNumber((byte) number);
    };

    // Set class number
    public SpanQueryWrapper setClassNumber (int number) {
	return this.setClassNumber((byte) number);
    };

    public String toString () {
	String string = "" +
	    (this.isNull() ? "isNull" : "notNull") +
	    "-" +
	    (this.isEmpty() ? "isEmpty" : "notEmpty") +
	    "-" +
	    (this.isOptional() ? "isOptional" : "notOptional");
	return string;
    };
};
