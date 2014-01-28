package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.ElementDistanceSpan;
import de.ids_mannheim.korap.query.spans.TokenDistanceSpan;

/** Match two ordered Spans with minimum and maximum distance constraints.
 * 	The distance unit can be word (token), sentence or paragraph. 
 * 
 * 	@author margaretha
 * */
public class SpanDistanceQuery extends SimpleSpanQuery {
	
	protected int minDistance, maxDistance;
	protected boolean collectPayloads;
	protected SpanQuery firstClause, secondClause; 
	private SpanQuery elementQuery; // element distance unit
	
	public SpanDistanceQuery(SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, boolean collectPayloads) {
		super(firstClause, secondClause, "spanDistance");		
    	this.firstClause=firstClause;
    	this.secondClause=secondClause;
    	this.minDistance =minDistance;
		this.maxDistance = maxDistance;
		this.collectPayloads = collectPayloads;
	}
	
	public SpanDistanceQuery(SpanQuery elementQuery, 
			SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, 
			boolean collectPayloads) {
		this(firstClause, secondClause,minDistance, maxDistance, 
				collectPayloads);
		this.elementQuery = elementQuery;    	    	
	}
	
	@Override
	public SpanDistanceQuery clone() {
		SpanDistanceQuery spanDistanceQuery = new SpanDistanceQuery(
		    (SpanQuery) firstClause.clone(),
		    (SpanQuery) secondClause.clone(),
		    this.minDistance,
		    this.maxDistance,
		    this.collectPayloads
        );
		
		if (this.elementQuery != null) {
			spanDistanceQuery.setElementQuery(this.elementQuery);
		}
		
		spanDistanceQuery.setBoost(getBoost());
		return spanDistanceQuery;	
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {
		
		if (this.elementQuery != null) 
			return new ElementDistanceSpan(this, context, acceptDocs, termContexts);		
		
		return new TokenDistanceSpan(this, context, acceptDocs, termContexts);
	}

	public int getMinDistance() {
		return minDistance;
	}

	public void setMinDistance(int minDistance) {
		this.minDistance = minDistance;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}

	public boolean isCollectPayloads() {
		return collectPayloads;
	}

	public void setCollectPayloads(boolean collectPayloads) {
		this.collectPayloads = collectPayloads;
	}

	
	public SpanQuery getElementQuery() {
		return elementQuery;
	}

	
	public void setElementQuery(SpanQuery elementQuery) {
		this.elementQuery = elementQuery;
	}

}
