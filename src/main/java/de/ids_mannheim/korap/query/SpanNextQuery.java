package de.ids_mannheim.korap.query;

// Based on SpanNearQuery

/*
  Todo: Make one Spanarray and switch between the results of A and B.
*/

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.spans.Spans;

import de.ids_mannheim.korap.query.spans.NextSpans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Matches spans which are directly next to each other. 
 * 	This is identical to a phrase query with exactly two clauses. 
 */
public class SpanNextQuery extends SimpleSpanQuery implements Cloneable {
    private SpanQuery firstClause;
    private SpanQuery secondClause;    
    private boolean collectPayloads;

    // Constructor
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause) {
    	this(firstClause, secondClause, true);
    };

    // Constructor  
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause,
		boolean collectPayloads) {   
    	super(firstClause, secondClause, "spanNext");
		this.collectPayloads = collectPayloads;
		this.firstClause = firstClause;
		this.secondClause = secondClause;
    };


    @Override
    public Spans getSpans (final AtomicReaderContext context, Bits acceptDocs,
		   Map<Term,TermContext> termContexts) throws IOException {	
		return (Spans) new NextSpans (this, context, acceptDocs, 
				termContexts, collectPayloads);
    };

    @Override
    public SpanNextQuery clone() {
	SpanNextQuery spanNextQuery = new SpanNextQuery(
	    (SpanQuery) firstClause.clone(),
	    (SpanQuery) secondClause.clone(),
	    this.collectPayloads
        );
	spanNextQuery.setBoost(getBoost());
	return spanNextQuery;
    };


    /** Returns true iff <code>o</code> is equal to this. */
    @Override
    public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof SpanNextQuery)) return false;
	
	final SpanNextQuery spanNextQuery = (SpanNextQuery) o;
	
	if (collectPayloads != spanNextQuery.collectPayloads) return false;
	if (!firstClause.equals(spanNextQuery.firstClause)) return false;
	if (!secondClause.equals(spanNextQuery.secondClause)) return false;

	return getBoost() == spanNextQuery.getBoost();
    };


    // I don't know what I am doing here
    @Override
    public int hashCode() {
	int result;
	result = firstClause.hashCode() + secondClause.hashCode();
	result ^= (result << 31) | (result >>> 2);  // reversible
	result += Float.floatToRawIntBits(getBoost());
	return result;
    };
};
