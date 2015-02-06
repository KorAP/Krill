package de.ids_mannheim.korap.model;

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
    public List<MultiTerm> terms;
    private short i = 0;
    private boolean sorted = false;

    /**
     * Construct a new MultiTermToken by passing a stream of
     * {@link MultiTerm MultiTerms}.
     *
     * @param terms Take at least one {@link MultiTerm} object for a token.
     */
    public MultiTermToken (MultiTerm terms, MultiTerm ... moreTerms) {
        this.terms = new ArrayList<MultiTerm>(16);
        
        this.terms.add( terms );

        // Further elements on same position
        for (i = 0; i < moreTerms.length; i++) {
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

        // First word element
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

        // First word element
        this.terms.add( term );

        // Further elements on same position
        for (i = 0; i < moreTerms.length; i++) {
            term = new MultiTerm( moreTerms[i] );
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
        terms.add(term);
        this.sorted = false;
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
        return this.add(new MultiTerm(term));
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
        return this.add(new MultiTerm(prefix, term));
    };


    /**
     * Get a {@link MultiTerm} by index.
     *
     * @param index The index position of a {@link MultiTerm}
     *        in the {@link MultiTermToken}.
     * @return A {@link MultiTerm}.
     */
    public MultiTerm get (int index) {
        return this.sort().terms.get(index);
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
     * Sort the {@link MultiTerm MultiTerms} in the correct order.
     *
     * @return The {@link MultiTermToken} object for chaining.
     */
    public MultiTermToken sort () {
        if (this.sorted)
            return this;

        Collections.sort(this.terms);
        this.sorted = true;
        return this;
    };


    /**
     * Serialize the MultiTermToken to a string.
     *
     * @return A string representation of the MultiTermToken,
     *         with leading offset information.
     */
    public String toString () {
        this.sort();
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (i = 0; i < this.terms.size() - 1; i++) {
            sb.append(this.terms.get(i).toString()).append('|');
        };
        sb.append(this.terms.get(i).toString()).append(']');
        
        return sb.toString();
    };
};
