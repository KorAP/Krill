package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.ToStringUtils;

/** An abstract class for Spanquery having two clauses. 
 * 
 * 	@author margaretha
 * */
public abstract class SimpleSpanQuery extends SpanQuery implements Cloneable{		
	
	private SpanQuery firstClause, secondClause;
	private String field;
	private String spanName;
    
    public SimpleSpanQuery(SpanQuery firstClause, SpanQuery secondClause, String spanName) {
    	this.field = secondClause.getField();
    	if (!firstClause.getField().equals(field)){
    		throw new IllegalArgumentException("Clauses must have the same field.");
    	}    	
    	this.setFirstClause(firstClause);
    	this.setSecondClause(secondClause);    	
    	this.spanName=spanName;
	}  
    	
	@Override
	public String getField() {
		return field;
	}

	@Override
	public String toString(String field) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.spanName);
		sb.append("(");
		sb.append(firstClause.toString(field));
	        sb.append(", ");
		sb.append(secondClause.toString(field));
		sb.append(")");
		sb.append(ToStringUtils.boost(getBoost()));
		return sb.toString();		
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
	
	// For rewriting fuzzy searches like wildcard and regex
	
	@Override
    public void extractTerms(Set<Term> terms) {
		firstClause.extractTerms(terms);
		secondClause.extractTerms(terms);
    };
    
	@Override
	public Query rewrite(IndexReader reader) throws IOException {		
		SimpleSpanQuery clone = clone();
		SpanQuery query = (SpanQuery) firstClause.rewrite(reader);
		if (!query.equals(firstClause)) {
	    	clone.firstClause = query;
		}		
		query = (SpanQuery) secondClause.rewrite(reader);
		if (!query.equals(secondClause)) {		    
		    clone.secondClause = query;
		}
		return (clone != null ? clone : this );		
	}	
	
	public abstract SimpleSpanQuery clone();	
	
}
