package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;

/** An abstract class for a Spanquery having two clauses. 
 * 
 * 	@author margaretha
 * */
public abstract class SimpleSpanQuery extends SpanQuery implements Cloneable{		
	
	protected SpanQuery firstClause, secondClause;
	private String field;
	protected boolean collectPayloads;
    
    public SimpleSpanQuery(SpanQuery firstClause, SpanQuery secondClause, 
    		boolean collectPayloads) {
    	this.field = secondClause.getField();
    	if (!firstClause.getField().equals(field)){
    		throw new IllegalArgumentException("Clauses must have the same field.");
    	}    	
    	this.setFirstClause(firstClause);
    	this.setSecondClause(secondClause);  	
    	this.collectPayloads = collectPayloads;
	}  
    	
	@Override
	public String getField() {
		return field;
	}
	
	public SpanQuery getFirstClause() {
		return firstClause;
	}

	public void setFirstClause(SpanQuery firstClause) {
		this.firstClause = firstClause;
	}

	public SpanQuery getSecondClause() {
		return secondClause;
	}

	public void setSecondClause(SpanQuery secondClause) {
		this.secondClause = secondClause;
	}	
		
	public boolean isCollectPayloads() {
		return collectPayloads;
	}

	public void setCollectPayloads(boolean collectPayloads) {
		this.collectPayloads = collectPayloads;
	}

	// For rewriting fuzzy searches like wildcard and regex
	@Override
    public void extractTerms(Set<Term> terms) {
		firstClause.extractTerms(terms);
		secondClause.extractTerms(terms);
    };
    
	@Override
	public Query rewrite(IndexReader reader) throws IOException {		
		SimpleSpanQuery clone = null;
		SpanQuery query = (SpanQuery) firstClause.rewrite(reader);
		if (!query.equals(firstClause)) {
			if (clone == null) clone = clone();
	    	clone.firstClause = query;
		}		
		query = (SpanQuery) secondClause.rewrite(reader);
		if (!query.equals(secondClause)) {		
			if (clone == null) clone = clone();
		    clone.secondClause = query;
		}
		return (clone != null ? clone : this );		
	}	
	
	public abstract SimpleSpanQuery clone();	
	
}
