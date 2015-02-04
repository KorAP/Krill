package de.ids_mannheim.korap.analysis;

import de.ids_mannheim.korap.analysis.MultiTerm;
import java.util.*;


/**
 *
 * A MultiTermToken represents a set of {@link MultiTerm MultiTerms}
 * starting at the same position, i.e. represents a segment
 * in a {@link MultiTermTokenStream}.
 *
 * <blockquote><pre>
 *  MultiTermToken mtt = new MultiTermToken("t:test", "a:abbruch");
 *  mtt.add("b:banane");
 *  System.err.println(mtt.toString());
 *  // [t:test|a:abbruch|b:banane]
 * </pre></blockquote>
 *
 * @author diewald
 */
public class MultiTermToken {
    public int start, end = 0;
    public List<MultiTerm> terms;
    private static short i = 0;


    /**
     * Construct a new MultiTermToken by passing a stream of
     * {@link MultiTerm MultiTerms}.
     *
     * @param terms Take at least one {@link MultiTerm} object for a token.
     */
    public MultiTermToken (MultiTerm terms, MultiTerm ... moreTerms) {
        this.terms = new ArrayList<MultiTerm>(16);
        
        // Start position is not equal to end position
        if (terms.start != terms.end) {
            this.start = terms.start;
            this.end   = terms.end;
        };

        terms.posIncr = 1;
        this.terms.add( terms );

        // Further elements on same position
        for (i = 0; i < moreTerms.length; i++) {
            moreTerms[i].posIncr = 0;
            this.terms.add(moreTerms[i]);
        };
    };


    /**
     * Construct a new MultiTermToken by passing a {@link MultiTerm}
     * represented as a prefixed string.
     *
     * @param prefix The term prefix.
     * @param surface The term surface.
     * @see MultiTerm
     */
    public MultiTermToken (char prefix, String surface) {
        this.terms = new ArrayList<MultiTerm>(16);

        // Create a new MultiTerm
        MultiTerm term = new MultiTerm(prefix, surface);

        this.setOffset(term.start, term.end);
        
        // First word element
        term.posIncr = 1;
        terms.add( term );
    };
    

    /**
     * Construct a new MultiTermToken by passing a stream of
     * {@link MultiTerm MultiTerms} represented as strings.
     *
     * @param terms Take at least one {@link MultiTerm} string for a token.
     */
    public MultiTermToken (String terms, String ... moreTerms) {
        this.terms = new ArrayList<MultiTerm>(16);

        MultiTerm term = new MultiTerm(terms);
        this.setOffset(term.start, term.end);

        // First word element
        term.posIncr = 1;
        this.terms.add( term );

        // Further elements on same position
        for (i = 0; i < moreTerms.length; i++) {
            term = new MultiTerm( moreTerms[i] );
            this.setOffset(term.start, term.end);
            term.posIncr = 0;
            this.terms.add(term);
        };
    };

    
    /**
     * Add a new {@link MultiTerm} to the MultiTermToken.
     *
     * @param term A {@link MultiTerm} object.
     * @return The {@link MultiTermToken} object for chaining.
     */
    public MultiTermToken add (MultiTerm term) {
        term.posIncr = 0;
        this.setOffset(term.start, term.end);
        terms.add(term);
        return this;
    };


    /**
     * Add a new {@link MultiTerm} to the MultiTermToken.
     *
     * @param term A MultiTerm represented as a surface string.
     * @return The {@link MultiTermToken} object for chaining.
     */
    public MultiTermToken add (String term) {
        if (term.length() == 0)
            return this;
        MultiTerm mt = new MultiTerm(term);
        this.setOffset(mt.start, mt.end);
        mt.posIncr = 0;
        terms.add(mt);
        return this;
    };


    /**
     * Add a new {@link MultiTerm} to the MultiTermToken.
     *
     * @param prefix A MultiTerm prefix.
     * @param term A MultiTerm represented as a surface string.
     * @return The {@link MultiTermToken} object for chaining.
     */
    public MultiTermToken add (char prefix, String term) {
        if (term.length() == 0)
            return this;
        MultiTerm mt = new MultiTerm(prefix, term);
        this.setOffset(mt.start, mt.end);
        mt.posIncr = 0;
        terms.add(mt);
        return this;
    };


    /**
     * Set the start and end character offset information
     * of the MultiTermToken.
     *
     * @param start The character position of the token start.
     * @param end The character position of the token end.
     * @return The {@link MultiTermToken} object for chaining.
     */
    public MultiTermToken setOffset (int start, int end) {

        // No value to set - offsets indicating a null string
        if (start != end) {
            this.start =
                (this.start == 0 || start < this.start) ?
                start : this.start;

            this.end = end > this.end ? end : this.end;
        };

        return this;
    };


    /**
     * Get the number of {@link MultiTerm MultiTerms}
     * in the MultiTermToken.
     *
     * @return The number of {@link MultiTerm MultiTerms}
     *         in the MultiTermToken.
     */
    public int getSize () {
        return this.terms.size();
    };



    /**
     * Serialize the MultiTermToken to a string.
     *
     * @return A string representation of the MultiTermToken,
     *         with leading offset information.
     */
    public String toString () {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        if (this.start != this.end) {
            sb.append('(')
                .append(this.start)
                .append('-')
                .append(this.end)
                .append(')');
        };

        for (i = 0; i < this.terms.size() - 1; i++) {
            sb.append(this.terms.get(i).toString()).append('|');
        };
        sb.append(this.terms.get(i).toString()).append(']');
        
        return sb.toString();
    };
};
