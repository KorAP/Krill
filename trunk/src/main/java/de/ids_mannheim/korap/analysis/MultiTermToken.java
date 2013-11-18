package de.ids_mannheim.korap.analysis;

import de.ids_mannheim.korap.analysis.MultiTerm;
import java.util.*;

/*
  Todo:
  - Always write offsets to payloads!
  - Offsets can be overwritten!
  - Check that terms are not ""!!!
*/

/**
 * @author Nils Diewald
 *
 * MultiTermToken represents a segment in a MultiTermTokenStream.
 */
public class MultiTermToken {
    public int start, end = 0;
    public List<MultiTerm> terms;

    public MultiTermToken (MultiTerm term, MultiTerm ... moreTerms) {
	this.terms = new ArrayList<MultiTerm>();

	if (term.start != term.end) {
	    this.start = term.start;
	    this.end = term.end;
	};

	term.posIncr = 1;
	terms.add( term );

	// Further elements on same position
	for (int i = 0; i < moreTerms.length; i++) {
	    term = moreTerms[i];
	    term.posIncr = 0;
	    terms.add(term);
	};
    };

    public MultiTermToken (char prefix, String surface) {
	this.terms = new ArrayList<MultiTerm>();

	MultiTerm term = new MultiTerm(prefix, surface);

	if (term.start != term.end) {
	    this.start = term.start;
	    this.end = term.end;
	};

	// First word element
	term.posIncr = 1;
	terms.add( term );
    };


    public MultiTermToken (String surface, String ... moreTerms) {
	this.terms = new ArrayList<MultiTerm>();

	MultiTerm term = new MultiTerm(surface);

	if (term.start != term.end) {
	    this.start = term.start;
	    this.end = term.end;
	};

	// First word element
	term.posIncr = 1;
	terms.add( term );


	// Further elements on same position
	for (int i = 0; i < moreTerms.length; i++) {

	    term = new MultiTerm( moreTerms[i] );
	    term.posIncr = 0;
	    terms.add(term);
	};
    };

    public void add (MultiTerm mt) {
	terms.add(mt);
    };

    public void add (String term) {
	MultiTerm mt = new MultiTerm(term);
	mt.posIncr = 0;
	terms.add(mt);
    };

    public void add (char prefix, String term) {
	MultiTerm mt = new MultiTerm(prefix, term);
	mt.posIncr = 0;
	terms.add(mt);
    };

    public void offset (int start, int end) {
	this.start = start;
	this.end   = end;
    };

    public String toString () {
	StringBuffer sb = new StringBuffer();

	sb.append('[');
	if (this.start != this.end) {
	    sb.append('(').append(this.start).append('-').append(this.end).append(')');
	};

	int i = 0;
	for (; i < this.terms.size() - 1; i++) {
	    sb.append(this.terms.get(i).toStringShort()).append('|');
	};
	sb.append(this.terms.get(i).toStringShort()).append(']');

	return sb.toString();
    };

    public int size () {
	return this.terms.size();
    };
};
