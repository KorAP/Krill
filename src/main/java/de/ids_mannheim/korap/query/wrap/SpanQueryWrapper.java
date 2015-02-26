package de.ids_mannheim.korap.query.wrap;

import org.apache.lucene.search.spans.SpanQuery;

import de.ids_mannheim.korap.util.QueryException;

// TODO: Add warnings and errors - using KrillQuery

/**
 * A wrapper base class for Lucene SpanQueries,
 * that add certain information,
 * necessary for the correct and optimized
 * deserialization of nested queries.
 *
 * This class is meant to be extended by
 * wrapper classes.
 *
 * @author diewald
 */
public class SpanQueryWrapper {

    // Boundaries, e.g. for repetitions
    protected int min = 1, max = 1;

    // Class number
    protected byte number = (byte) 0;

    // Boolean properties
    protected boolean
        hasClass   = false,
        isNull     = true,
        isOptional = false,
        isNegative = false,
        isEmpty    = false,
        isExtended = false,
        isExtendedToTheRight = false,
        maybeUnsorted = false,
        retrieveNode = false;

    /**
     * Serialize the wrapped query and return a SpanQuery.
     * This should be overwritten.
     *
     * @return A {@link SpanQuery} object.
     * @throws QueryException
     */
    public SpanQuery toQuery () throws QueryException {
        return (SpanQuery) null;
    };


    /**
     * Boolean value indicating that the wrapped query
     * is optional.
     *
     * For example the segment denoting an adjective
     * in the following Poliqarp expression is optional.
     *
     * <blockquote><pre>
     *   the [pos=ADJ]? tree
     * </pre></blockquote>
     *
     * @return <tt>true</tt> in case the wrapped query is
     *         optional and <tt>false</tt> in case it is
     *         mandatory.
     */
    public boolean isOptional () {
        return this.isOptional;
    };


    /**
     * Boolean value indicating that the wrapped query is
     * <tt>null</tt>, meaning it doesn't match anything at
     * all.
     *
     * For example the segment denoting an adjective
     * in the following Poliqarp expression doen't match
     * anything.
     *
     * <blockquote><pre>
     *   the [pos=ADJ]{0} tree
     * </pre></blockquote>
     *
     * @return <tt>true</tt> in case the wrapped query can't
     *         match anything, otherwise <tt>false</tt>.
     */
    public boolean isNull () {
        if (this.getMin() == 0 && this.getMax() == 0)
            return true;
        return this.isNull;
    };


    /**
     * Boolean value indicating that the wrapped query matches
     * in case the condition of the query is not true.
     *
     * For example the segment denoting an adjective
     * in the following Poliqarp expression is negative.
     *
     * <blockquote><pre>
     *   the [pos!=ADJ]
     * </pre></blockquote>
     *
     * @return <tt>true</tt> in case the wrapped query is
     *         negative, otherwise <tt>false</tt>.
     */
    public boolean isNegative () {
        return this.isNegative;
    };

    /**
     * Boolean value indicating that the wrapped query has
     * no further condition for matching and therefore
     * matches everywhere.
     *
     * For example the empty segment in the following
     * Poliqarp expression matches without any condition.
     *
     * <blockquote><pre>
     *   the []
     * </pre></blockquote>
     *
     * @return <tt>true</tt> in case the wrapped query is
     *         empty, otherwise <tt>false</tt>.
     */
    public boolean isEmpty () {
        return this.isEmpty;
    };


    /**
     * Boolean value indicating that the wrapped query
     * is extended by subquery.
     *
     * For example the segment denoting an adjective may
     * be wrapped as having an extension to the left.
     *
     * <blockquote><pre>
     *   []{3,4}[base=tree]
     * </pre></blockquote>
     *
     * @return <tt>true</tt> in case the wrapped query is
     *         extended, otherwise <tt>false</tt>.
     */
    public boolean isExtended () {
        return this.isExtended;
    };


    /**
     * Boolean value indicating that the wrapped query
     * is extended by a subquery to the right.
     *
     * For example the segment denoting the lemma tree
     * may be wrapped as being extended to the right
     * in the following Poliqarp expression.
     *
     * <blockquote><pre>
     *   [base=tree][]{3,4}
     * </pre></blockquote>
     *
     * This information is necessary to ensure a match
     * is valid even at the end of a document.
     *
     * @return <tt>true</tt> in case the wrapped query is
     *         extended to the right, otherwise <tt>false</tt>.
     */
    public boolean isExtendedToTheRight () {
        return this.isExtendedToTheRight;
    };


    /**
     * Check, if the wrapped query can be used as an
     * anchor query in a sequence, i.e. a query that
     * has a condition that must be positively evaluated.
     *
     * Wrapped queries with positive conditions are neither
     * negative, optional, nor empty.
     *
     * This is the opposite of {@link #maybeExtension}.
     *
     * @return <tt>true</tt> in case the wrapped query
     *         can be used as an anchor in a sequence,
     *         otherwise <tt>false</tt>.
     * @see SpanSequenceQueryWrapper
     */
    public boolean maybeAnchor () {
        if (this.isNegative()) return false;
        if (this.isOptional()) return false;
		if (this.isEmpty()) { return false;}		
        return true;
    };


    /**
     * Check, if the wrapped query can't be used as an
     * anchor query in a sequence, meaning it has to be
     * constructed as an extension to an anchor query.
     *
     * Wrapped queries with negative conditions are either
     * negative, optional, or empty.
     *
     * This is the opposite of {@link #maybeAnchor}.
     *
     * @return <tt>true</tt> in case the wrapped query
     *         has to be used as an extension in a sequence,
     *         otherwise <tt>false</tt>.
     * @see SpanSequenceQueryWrapper
     */
    public boolean maybeExtension () {
        return !this.maybeAnchor();
    };


    /**
     * Check, if the wrapped query may need to be sorted
     * on focussing on a specific class.
     *
     * Normally spans are always sorted, but in case of
     * a wrapped relation query, classed operands may
     * be in arbitrary order. When focussing on these
     * classes, the span has to me reordered.
     *
     * @return <tt>true</tt> in case the wrapped query
     *         has to be sorted on focussing,
     *         otherwise <tt>false</tt>.
     */
    public boolean maybeUnsorted () {
        return this.maybeUnsorted;
    };


    /**
     * Get the minimum number of repetitions of the
     * wrapped query.
     *
     * @return The minimum number of repetions.
     * @see SpanRepetitionQueryWrapper
     */
    public int getMin () {
        return this.min;
    };


    /**
     * Set the minimum number of repetitions of the
     * wrapped query.
     *
     * @param min The minimum number of repetions.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper setMin (int min) {
        this.min = min;
        return this;
    };


    /**
     * Get the maximum number of repetitions of the
     * wrapped query.
     *
     * @return The maximum number of repetions.
     * @see SpanRepetitionQueryWrapper
     */
    public int getMax () {
        return this.max;
    };


    /**
     * Set the maximum number of repetitions of the
     * wrapped query.
     *
     * @param max The maximum number of repetions.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper setMax (int max) {
        this.max = max;
        return this;
    };


    /**
     * Make the query request node information in addition to
     * span information.
     *
     * @param retrieve Boolean value saying the wrapper
     *        has or has not to respect node information.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper retrieveNode (boolean retrieve) {
        this.retrieveNode = retrieve;
        return this;
    };


    /**
     * Boolean value indicating that a wrapped query
     * has a class. This is especially relevant for classed
     * extension queries.
     */
    public boolean hasClass () {
        return this.hasClass;
    };


    /**
     * Get the class number, if set.
     * Returns <tt>0</tt> in case no class is set.
     *
     * @return The class number.
     */
    public byte getClassNumber () {
        return this.number;
    };


    /**
     * Set the class number.
     *
     * @param number The class number as a byte value.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper setClassNumber (byte number) {
        this.hasClass = true;
        this.number = number;
        return this;
    };


    /**
     * Set the class number.
     *
     * @param number The class number as a short value.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper setClassNumber (short number) {
        return this.setClassNumber((byte) number);
    };


    /**
     * Set the class number.
     *
     * @param number The class number as an integer value.
     * @return The {@link SpanQueryWrapper} object for chaining.
     */
    public SpanQueryWrapper setClassNumber (int number) {
        return this.setClassNumber((byte) number);
    };


    /**
     * Serialize the wrapped query to a string representation.
     *
     * This is meant to be overwritten.
     *
     * @return A string containg the query representation.
     */
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
