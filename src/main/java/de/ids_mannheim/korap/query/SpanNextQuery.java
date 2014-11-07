package de.ids_mannheim.korap.query;

// Based on SpanNearQuery

/*
  Todo: Make one Spanarray and switch between the results of A and B.
*/

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;


import de.ids_mannheim.korap.query.spans.NextSpans;

/**
 * Matches spans which are directly next to each other. 
 *  this is identical to a phrase query with exactly two clauses. 
 */
public class SpanNextQuery extends SimpleSpanQuery implements Cloneable {

	// Constructor
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause) {
    	this(firstClause, secondClause, true);
    };

    // Constructor  
    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause,
		boolean collectPayloads) {   
    	super(firstClause, secondClause, collectPayloads);
    };

    public SpanNextQuery(SpanQuery firstClause, SpanQuery secondClause, 
    		boolean isFirstNegated, boolean collectPayloads) {   
    	super(firstClause, secondClause, collectPayloads);
    }
    
    
    @Override
    public Spans getSpans (final AtomicReaderContext context, Bits acceptDocs,
		   Map<Term,TermContext> termContexts) throws IOException {	
		return (Spans) new NextSpans (this, context, acceptDocs, termContexts);
    };

    @Override
    public SpanNextQuery clone() {
	SpanNextQuery spanNextQuery = new SpanNextQuery(
	    (SpanQuery) firstClause.clone(),
	    (SpanQuery) secondClause.clone(),
	    collectPayloads
        );
	spanNextQuery.setBoost(getBoost());
	return spanNextQuery;
    };
   

    /*
     * Rewrite query in case it includes regular expressions or wildcards
     */
    @Override
    public Query rewrite (IndexReader reader) throws IOException {
	SpanNextQuery clone = null;

	// Does the first clause needs a rewrite?
	SpanQuery query = (SpanQuery) firstClause.rewrite(reader);
	if (query != firstClause) {
	    if (clone == null)
		clone = this.clone();
	    clone.firstClause = query;
	};

	// Does the second clause needs a rewrite?
	query = (SpanQuery) secondClause.rewrite(reader);
	if (query != secondClause) {
	    if (clone == null)
		clone = this.clone();
	    clone.secondClause = query;
	};

	// There is a clone and it is important
	if (clone != null)
	    return clone;

	return this;
    };

 
    @Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append("spanNext(");
		sb.append(firstClause.toString(field));
	        sb.append(", ");
		sb.append(secondClause.toString(field));
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();	
    }


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
