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
 */
public class SpanNextQuery extends SpanQuery implements Cloneable {
    private SpanQuery firstClause;
    private SpanQuery secondClause;
    public String field;
    private boolean collectPayloads;

    // Logger
    private final static Logger log = LoggerFactory.getLogger(SpanNextQuery.class);

    // Constructor
    public SpanNextQuery(SpanQuery firstClause,
			 SpanQuery secondClause) {
	this(firstClause, secondClause, true);
    };

    // Constructor  
    public SpanNextQuery(SpanQuery firstClause,
			 SpanQuery secondClause,
			 boolean collectPayloads) {

	this.field = secondClause.getField();
	if (!firstClause.getField().equals(field)) {
	    throw new IllegalArgumentException("Clauses must have same field.");
	};

	this.collectPayloads = collectPayloads;
	this.firstClause = firstClause;
	this.secondClause = secondClause;
    };


    @Override
    public String getField() { return field; }

    public SpanQuery firstClause() { return firstClause; };

    public SpanQuery secondClause() { return secondClause; };
  
    @Override
    public void extractTerms(Set<Term> terms) {
	firstClause.extractTerms(terms);
	secondClause.extractTerms(terms);
    };
  

    @Override
    public String toString(String field) {
	StringBuilder buffer = new StringBuilder();
	buffer.append("spanNext(");
	buffer.append(firstClause.toString(field));
        buffer.append(", ");
	buffer.append(secondClause.toString(field));
	buffer.append(")");
	buffer.append(ToStringUtils.boost(getBoost()));
	return buffer.toString();
    };

    @Override
    public Spans getSpans (final AtomicReaderContext context,
			   Bits acceptDocs,
			   Map<Term,TermContext> termContexts) throws IOException {

	log.trace("Get Spans");
	return (Spans) new NextSpans (
            this, context, acceptDocs, termContexts, collectPayloads
	);
    };

    @Override
    public Query rewrite (IndexReader reader) throws IOException {
	// System.err.println(">> Rewrite query");

	SpanNextQuery clone = null;

	// System.err.println(">> Rewrite first clause");
	SpanQuery query = (SpanQuery) firstClause.rewrite(reader);

	if (query != firstClause) {
	    if (clone == null)
		clone = this.clone();
	    clone.firstClause = query;
	};

	// System.err.println(">> Rewrite second clause");
	query = (SpanQuery) secondClause.rewrite(reader);
	if (query != secondClause) {
	    if (clone == null)
		clone = this.clone();
	    clone.secondClause = query;
	};

	if (clone != null) {
	    // System.err.println(">> Clone is not null");
	    return clone;
	};

	return this;
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
