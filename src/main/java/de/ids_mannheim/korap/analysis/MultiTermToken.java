package de.ids_mannheim.korap.analysis;

import de.ids_mannheim.korap.analysis.MultiTerm;
import java.util.*;


/**
 * @author Nils Diewald
 *
 * MultiTermToken represents a segment in a MultiTermTokenStream.
 */
public class MultiTermToken {
    public int start, end = 0;
    public List<MultiTerm> terms;

    private static short i = 0;

    /**
     * The constructor.
     *
     * @param terms Take at least one MultiTerm object for a token.
     */
    public MultiTermToken (MultiTerm term, MultiTerm ... moreTerms) {
	this.terms = new ArrayList<MultiTerm>(16);

	if (term.start != term.end) {
	    this.start = term.start;
	    this.end = term.end;
	};

	term.posIncr = 1;
	terms.add( term );

	// Further elements on same position
	for (i = 0; i < moreTerms.length; i++) {
	    term = moreTerms[i];
	    term.posIncr = 0;
	    terms.add(term);
	};
    };


    /**
     * The constructor.
     *
     * @param prefix A term prefix.
     * @param surface A surface string.
     */
    public MultiTermToken (char prefix, String surface) {
	this.terms = new ArrayList<MultiTerm>(16);

	MultiTerm term = new MultiTerm(prefix, surface);

	this.setOffset(term.start, term.end);

	// First word element
	term.posIncr = 1;
	terms.add( term );
    };
    

    /**
     * The constructor.
     *
     * @param prefix At least one term surface string.
     */
    public MultiTermToken (String surface, String ... moreTerms) {
	this.terms = new ArrayList<MultiTerm>(16);

	MultiTerm term = new MultiTerm(surface);

	this.setOffset(term.start, term.end);

	// First word element
	term.posIncr = 1;
	terms.add( term );

	// Further elements on same position
	for (i = 0; i < moreTerms.length; i++) {
	    term = new MultiTerm( moreTerms[i] );
	    this.setOffset(term.start, term.end);
	    term.posIncr = 0;
	    terms.add(term);
	};
    };

    
    /**
     * Add a new term to the MultiTermToken.
     *
     * @param mt A MultiTerm.
     */
    public void add (MultiTerm mt) {
	mt.posIncr = 0;
	this.setOffset(mt.start, mt.end);
	terms.add(mt);
    };


    /**
     * Add a new term to the MultiTermToken.
     *
     * @param term A surface string.
     */
    public void add (String term) {
	if (term.length() == 0)
	    return;
	MultiTerm mt = new MultiTerm(term);
	this.setOffset(mt.start, mt.end);
	mt.posIncr = 0;
	terms.add(mt);
    };

    /**
     * Add a new term to the MultiTermToken.
     *
     * @param prefix A prefix character for the surface string.
     * @param term A surface string.
     */
    public void add (char prefix, String term) {
	if (term.length() == 0)
	    return;
	MultiTerm mt = new MultiTerm(prefix, term);
	this.setOffset(mt.start, mt.end);
	mt.posIncr = 0;
	terms.add(mt);
    };


    /**
     * Sets the offset information of the MultiTermToken.
     *
     * @param start The character position of the token start.
     * @param end The character position of the token end.
     */
    public void setOffset (int start, int end) {
	if (start != end) {
	    this.start = (this.start == 0 || start < this.start) ? start : this.start;
	    this.end   = end > this.end ? end : this.end;
	};
    };

    /**
     * Serialize the MultiTermToken to a string.
     *
     * @return A string representation of the token, with leading offset information.
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

	i = 0;
	for (; i < this.terms.size() - 1; i++) {
	    sb.append(this.terms.get(i).toString()).append('|');
	};
	sb.append(this.terms.get(i).toString()).append(']');

	return sb.toString();
    };

    /**
     * Return the number of MultiTerms in the MultiTermToken.
     */
    public int size () {
	return this.terms.size();
    };
};
