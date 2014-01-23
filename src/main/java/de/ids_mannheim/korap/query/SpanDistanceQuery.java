package de.ids_mannheim.korap.query;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;

import de.ids_mannheim.korap.query.spans.DistanceSpan;

/** Match two ordered Spans with minimum and maximum distance constraints.
 * 	In this query, the distance unit is the difference between two 
 * 	token positions.
 * 
 * 	@author margaretha
 * */
public class SpanDistanceQuery extends SimpleSpanQuery {
	
	private int minDistance, maxDistance; // token positions
	private boolean collectPayloads;
	private SpanQuery firstClause, secondClause; 
	
	public SpanDistanceQuery(SpanQuery firstClause, SpanQuery secondClause, 
			int minDistance, int maxDistance, boolean collectPayloads) {
		super(firstClause, secondClause, "spanDistance");		
    	this.firstClause=firstClause;
    	this.secondClause=secondClause;
    	this.minDistance =minDistance;
		this.maxDistance = maxDistance;
		this.collectPayloads = collectPayloads;
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
		spanDistanceQuery.setBoost(getBoost());
		return spanDistanceQuery;	
	}

	@Override
	public Spans getSpans(AtomicReaderContext context, Bits acceptDocs,
			Map<Term, TermContext> termContexts) throws IOException {		
		return new DistanceSpan(this, context, acceptDocs, termContexts);
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

}
