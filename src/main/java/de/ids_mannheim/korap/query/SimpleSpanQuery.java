package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;

/** A base class for Spanqueries 
 * 
 * 	@author margaretha
 * */
public abstract class SimpleSpanQuery extends SpanQuery 
		implements Cloneable{		
	
	protected SpanQuery firstClause, secondClause;
	protected List<SpanQuery> clauseList;
	private String field;
	protected boolean collectPayloads;
    
	public SimpleSpanQuery(SpanQuery firstClause, boolean collectPayloads) {
    	this.field = firstClause.getField();
    	this.setFirstClause(firstClause);
    	this.collectPayloads = collectPayloads;
	}  
	
    public SimpleSpanQuery(SpanQuery firstClause, SpanQuery secondClause, 
    		boolean collectPayloads) {
    	this(firstClause,collectPayloads);
    	checkField(secondClause);
    	this.setSecondClause(secondClause);  	
	}
    
    public SimpleSpanQuery(SpanQuery firstClause, List<SpanQuery> 
    		secondClauses, boolean collectPayloads) {
    	this(firstClause,collectPayloads);
    	for (SpanQuery secondClause : secondClauses){
	    	checkField(secondClause);
		}
    	this.setClauseList(secondClauses);
	}
    
    private void checkField(SpanQuery clause) {
    	if (!clause.getField().equals(field)){
    		throw new IllegalArgumentException(
    				"Clauses must have the same field.");
    	}
    }
	
	public List<SpanQuery> getClauseList() {
		return clauseList;
	}

	public void setClauseList(List<SpanQuery> clauseList) {
		this.clauseList = clauseList;
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
		if (secondClause != null){
			secondClause.extractTerms(terms);
		}
		else if (clauseList != null){
			for (SpanQuery clause : clauseList){
				clause.extractTerms(terms);
			}
		}
			
    };
    
	@Override
	public Query rewrite(IndexReader reader) throws IOException {		
		SimpleSpanQuery clone = null;
		clone = updateClone(reader, clone, firstClause, 1);			
		if (secondClause != null){
		    clone = updateClone(reader, clone, secondClause, 2);			
		}
		else if (clauseList != null){
			clone = updateClone(reader, clone, clauseList);
		}
		return (clone != null ? clone : this );		
	}	
	
	private SimpleSpanQuery updateClone(IndexReader reader, SimpleSpanQuery clone, 
			 List<SpanQuery> spanQueries) throws IOException{
		
		for (int i=0; i < spanQueries.size(); i++){
			SpanQuery query = (SpanQuery) spanQueries.get(i).rewrite(reader);
			if (!query.equals(spanQueries.get(i))) {
				if (clone == null) clone = clone();
				clone.getClauseList().set(i, query);
			}
		}
		return clone;
	}
	
	private SimpleSpanQuery updateClone(IndexReader reader, SimpleSpanQuery clone, 
			 SpanQuery sq, int clauseNumber) throws IOException{
		SpanQuery query = (SpanQuery) sq.rewrite(reader);
		if (!query.equals(sq)) {
			if (clone == null) clone = clone();
			if (clauseNumber == 1) 
				clone.firstClause = query;			
			else clone.secondClause = query;
		}
		return clone;
	}
	
	public abstract SimpleSpanQuery clone();	
	
}
